/*!
 * 签名板组件（SignaturePad）
 * 使用方：report.html（工单验收签字 + 月度报销单签字）
 *
 * 修复的关键问题（原实现）：
 *   1) 画布内在像素（780×260）与 CSS 显示尺寸（100%×140px）不一致，
 *      而绘制用 offsetX/offsetY（CSS 像素）→ 笔迹只落在左上角约 40%，导出图又小又偏；
 *      现在按「显示尺寸 × devicePixelRatio」设置像素，并用 setTransform 把坐标系对齐到 CSS 像素。
 *   2) 未设置线宽/笔头 → 默认 1px 直角，鼠标写出来像头发丝；
 *      现在 lineWidth 可配 + 圆头圆角。
 *   3) 无撤销、无引导、高分屏模糊；
 *      现在支持逐笔撤销、引导层（CSS 层，不进导出图）、DPR 适配。
 *   附加：中点二次贝塞尔平滑（鼠标事件稀疏也不出折线）、导出前自动裁剪空白+留白。
 *
 * 两种模式：
 *   HANDWRITTEN 手写（默认，医院正式流程）
 *   TYPED       输入姓名生成（无触屏 PC 的便捷模式，签字方式会被记录以便审计）
 */
(function (global) {
  'use strict';

  function dpr() {
    return Math.max(1, Math.min(3, global.devicePixelRatio || 1));
  }

  function create(opts) {
    var canvas = opts.canvas;
    var ctx = canvas.getContext('2d');
    var lineWidth = opts.lineWidth || 3;
    var color = opts.color || '#1f2937';
    var padCss = opts.padding === undefined ? 14 : opts.padding;

    var mode = 'HANDWRITTEN';
    var strokes = [];     // 已完成的笔画：[[{x,y}...], ...]（CSS 像素坐标）
    var current = null;   // 正在画的笔画
    var typedName = '';
    var destroyed = false;

    /* ---------------- 尺寸与重绘 ---------------- */
    function logicalSize() {
      var rect = canvas.getBoundingClientRect ? canvas.getBoundingClientRect() : null;
      var w = Math.round((rect && rect.width) || canvas.clientWidth || 320);
      var h = Math.round((rect && rect.height) || canvas.clientHeight || 170);
      return { w: Math.max(1, w), h: Math.max(1, h) };
    }

    function resize() {
      if (destroyed) return;
      var size = logicalSize();
      var ratio = dpr();
      canvas.width = Math.round(size.w * ratio);
      canvas.height = Math.round(size.h * ratio);
      // 之后所有绘制都使用 CSS 像素坐标，pointer 坐标可直接使用
      ctx.setTransform(ratio, 0, 0, ratio, 0, 0);
      redraw();
    }

    function redraw() {
      if (destroyed) return;
      var size = logicalSize();
      ctx.clearRect(0, 0, size.w, size.h);
      ctx.lineWidth = lineWidth;
      ctx.lineCap = 'round';
      ctx.lineJoin = 'round';
      ctx.strokeStyle = color;
      ctx.fillStyle = color;
      if (mode === 'TYPED') {
        drawTyped(size);
      } else {
        strokes.forEach(drawStroke);
        if (current) drawStroke(current);
      }
      notify();
    }

    /** 中点二次贝塞尔平滑，避免鼠标采样稀疏时出现折线 */
    function drawStroke(points) {
      if (!points || !points.length) return;
      if (points.length === 1) {
        ctx.beginPath();
        ctx.arc(points[0].x, points[0].y, lineWidth / 2, 0, Math.PI * 2);
        ctx.fill();
        return;
      }
      ctx.beginPath();
      ctx.moveTo(points[0].x, points[0].y);
      for (var i = 1; i < points.length - 1; i++) {
        var mx = (points[i].x + points[i + 1].x) / 2;
        var my = (points[i].y + points[i + 1].y) / 2;
        ctx.quadraticCurveTo(points[i].x, points[i].y, mx, my);
      }
      var last = points[points.length - 1];
      ctx.lineTo(last.x, last.y);
      ctx.stroke();
    }

    /** 输入姓名生成：楷体斜体，字号自适应宽度并限制高度 */
    function drawTyped(size) {
      var text = typedName.trim();
      if (!text) return;
      var families = '"KaiTi","STKaiti","Kaiti SC","楷体","Microsoft YaHei",cursive';
      var font = Math.min(size.h * 0.5, size.w * 0.16);
      ctx.textAlign = 'center';
      ctx.textBaseline = 'middle';
      for (var i = 0; i < 14; i++) {
        ctx.font = 'italic ' + Math.round(font) + 'px ' + families;
        if (ctx.measureText(text).width <= size.w * 0.86) break;
        font *= 0.88;
      }
      ctx.fillText(text, size.w / 2, size.h / 2);
    }

    /* ---------------- 指针交互 ---------------- */
    function pointOf(e) {
      var rect = canvas.getBoundingClientRect();
      return { x: e.clientX - rect.left, y: e.clientY - rect.top };
    }

    function onDown(e) {
      if (mode !== 'HANDWRITTEN') return;
      if (canvas.setPointerCapture && e.pointerId !== undefined) {
        try { canvas.setPointerCapture(e.pointerId); } catch (err) { /* 某些浏览器忽略 */ }
      }
      current = [pointOf(e)];
      redraw();
    }

    function onMove(e) {
      if (mode !== 'HANDWRITTEN' || !current) return;
      var events = (typeof e.getCoalescedEvents === 'function') ? e.getCoalescedEvents() : null;
      if (events && events.length) {
        for (var i = 0; i < events.length; i++) current.push(pointOf(events[i]));
      } else {
        current.push(pointOf(e));
      }
      redraw();
    }

    function onUp() {
      if (mode !== 'HANDWRITTEN' || !current) return;
      strokes.push(current);
      current = null;
      redraw();
    }

    canvas.addEventListener('pointerdown', onDown);
    canvas.addEventListener('pointermove', onMove);
    canvas.addEventListener('pointerup', onUp);
    canvas.addEventListener('pointercancel', onUp);
    canvas.addEventListener('pointerleave', onUp);

    var resizeTimer = null;
    function onWindowResize() {
      if (resizeTimer) global.clearTimeout(resizeTimer);
      resizeTimer = global.setTimeout(resize, 120);
    }
    global.addEventListener('resize', onWindowResize);

    /* ---------------- 导出 ---------------- */
    /** 内容包围盒（设备像素）；无内容返回 null。测试可直接调用。 */
    function contentBox() {
      var w = canvas.width, h = canvas.height;
      if (!w || !h) return null;
      var data = ctx.getImageData(0, 0, w, h).data;
      var minX = w, minY = h, maxX = -1, maxY = -1;
      for (var y = 0; y < h; y++) {
        for (var x = 0; x < w; x++) {
          if (data[(y * w + x) * 4 + 3] > 8) {
            if (x < minX) minX = x;
            if (y < minY) minY = y;
            if (x > maxX) maxX = x;
            if (y > maxY) maxY = y;
          }
        }
      }
      if (maxX < 0) return null;
      return { x: minX, y: minY, w: maxX - minX + 1, h: maxY - minY + 1 };
    }

    /** 导出 PNG：自动裁剪空白 + 四周留白，得到紧凑居中的签名图 */
    function toBlob() {
      return new Promise(function (resolve) {
        var box = contentBox();
        if (!box) { resolve(null); return; }
        var p = Math.round(padCss * dpr());
        var sx = Math.max(0, box.x - p);
        var sy = Math.max(0, box.y - p);
        var ex = Math.min(canvas.width, box.x + box.w + p);
        var ey = Math.min(canvas.height, box.y + box.h + p);
        var out = global.document.createElement('canvas');
        out.width = Math.max(1, ex - sx);
        out.height = Math.max(1, ey - sy);
        var octx = out.getContext('2d');
        octx.drawImage(canvas, sx, sy, out.width, out.height, 0, 0, out.width, out.height);
        if (typeof out.toBlob !== 'function') { resolve(null); return; }
        out.toBlob(function (blob) { resolve(blob); }, 'image/png');
      });
    }

    /* ---------------- 对外 API ---------------- */
    function notify() {
      if (opts.onChange) {
        opts.onChange({ mode: mode, empty: isEmpty(), strokes: strokes.length, typed: typedName });
      }
    }

    function isEmpty() {
      return mode === 'TYPED' ? !typedName.trim() : strokes.length === 0;
    }

    function setMode(next) {
      var m = (next === 'TYPED') ? 'TYPED' : 'HANDWRITTEN';
      if (m === mode) return;
      mode = m;
      current = null;
      redraw();
    }

    function setTypedName(name) {
      typedName = String(name || '');
      if (mode !== 'TYPED') mode = 'TYPED';
      redraw();
    }

    function clear() {
      strokes = [];
      current = null;
      typedName = '';
      redraw();
    }

    function undo() {
      if (mode === 'TYPED') {
        typedName = '';
      } else {
        strokes.pop();
      }
      current = null;
      redraw();
    }

    function destroy() {
      destroyed = true;
      global.removeEventListener('resize', onWindowResize);
    }

    resize();

    return {
      resize: resize,
      clear: clear,
      undo: undo,
      destroy: destroy,
      isEmpty: isEmpty,
      toBlob: toBlob,
      contentBox: contentBox,
      setMode: setMode,
      getMode: function () { return mode; },
      setTypedName: setTypedName,
      strokeCount: function () { return strokes.length; },
      strokePoints: function () { return strokes.map(function (s) { return s.length; }); }
    };
  }

  global.SignaturePad = { create: create };
})(window);
