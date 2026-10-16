/*!
 * 后台 UI 原子组件（Admin.ui）
 * 目标：视图只关心数据与布局，不重复写卡片/表格/抽屉/分页/提示的样板代码。
 * 依赖：无（DOM 原生 + 少量内联 SVG），医院内网/离线可用。
 *
 * 约定：h() 用 textContent 渲染文本，天然防 XSS；
 *       若列 render 返回字符串，则按「可信 HTML」处理（内部拼接用，用户数据请先 esc()）。
 */
(function (global) {
  'use strict';
  var Admin = global.Admin = global.Admin || {};
  var doc = global.document;

  /* ---------------- 基础工具 ---------------- */
  function esc(s) {
    return (s === null || s === undefined ? '' : String(s)).replace(/[&<>"']/g, function (c) {
      return { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c];
    });
  }

  function append(node, child) {
    if (child === null || child === undefined || child === false || child === true) return;
    if (Array.isArray(child)) { child.forEach(function (c) { append(node, c); }); return; }
    if (child instanceof global.Node) { node.appendChild(child); return; }
    node.appendChild(doc.createTextNode(String(child)));
  }

  /**
   * DOM 构造器：h('div', { class:'x', onclick: fn, html:'<svg/>' }, child1, child2)
   */
  function h(tag, props) {
    var node = doc.createElement(tag);
    if (props) {
      Object.keys(props).forEach(function (k) {
        var v = props[k];
        if (v === null || v === undefined || v === false) return;
        if (k === 'class' || k === 'className') { node.className = v; return; }
        if (k === 'text') { node.textContent = v; return; }
        if (k === 'html') { node.innerHTML = v; return; }   // 仅供内置图标等可信内容
        if (k === 'style' && typeof v === 'object') { Object.assign(node.style, v); return; }
        if (k.indexOf('on') === 0 && typeof v === 'function') { node.addEventListener(k.slice(2).toLowerCase(), v); return; }
        if (k === 'dataset' && typeof v === 'object') { Object.assign(node.dataset, v); return; }
        node.setAttribute(k, v);
      });
    }
    for (var i = 2; i < arguments.length; i++) append(node, arguments[i]);
    return node;
  }

  function fmtTime(s) {
    if (!s) return '-';
    return String(s).replace('T', ' ').slice(0, 16);
  }
  function money(v) {
    if (v === null || v === undefined || v === '') return '¥0.00';
    return '¥' + Number(v).toFixed(2);
  }
  function fmtDate(s) { return s ? String(s).slice(0, 10) : '-'; }

  /* ---------------- 徽标 / 状态 ---------------- */
  function badge(text, tone) {
    return h('span', { class: 'badge' + (tone ? ' ' + tone : ''), text: text });
  }
  /**
   * 状态徽标：statusMap = { CODE: {label:'待受理', tone:'pending'} }
   * 兼容旧的 { CODE: '待受理' } 形式。
   */
  function statusBadge(statusMap, code) {
    var item = statusMap ? statusMap[code] : null;
    if (item === null || item === undefined) item = code || '-';
    var label = typeof item === 'string' ? item : item.label;
    var tone = typeof item === 'string' ? '' : item.tone;
    return badge(label, tone);
  }

  /* ---------------- 卡片 / 指标 ---------------- */
  function card(opts) {
    var head = null;
    if (opts.title || opts.extra) {
      head = h('div', { class: 'card-head' },
        opts.title ? h('h3', { text: opts.title }) : null,
        opts.hint ? h('span', { class: 'count-hint', text: opts.hint }) : null,
        opts.extra ? h('div', { class: 'card-extra' }, opts.extra) : null
      );
    }
    return h('div', { class: 'card' + (opts.className ? ' ' + opts.className : '') },
      head, opts.body || null);
  }

  function metricCard(opts) {
    var deltaNode = null;
    if (opts.delta) {
      deltaNode = h('span', { class: 'm-delta ' + (opts.delta.tone || ''), text: opts.delta.text });
    }
    return h('div', { class: 'metric' + (opts.tone ? ' tone-' + opts.tone : '') },
      h('div', { class: 'm-icon', html: opts.icon || '' }),
      h('div', { class: 'm-body' },
        h('div', { class: 'm-title', text: opts.title }),
        h('div', { class: 'm-value' }, String(opts.value),
          opts.unit ? h('span', { class: 'm-unit', text: opts.unit }) : null),
        h('div', { class: 'm-hint' }, opts.hint || '', deltaNode ? ' ' : null, deltaNode)
      )
    );
  }

  /* ---------------- 表格 ---------------- */
  function dataTable(opts) {
    var columns = opts.columns || [];
    var rows = opts.rows || [];
    var thead = h('thead', null, h('tr', null, columns.map(function (c) {
      return h('th', { class: (c.className || '') + (c.align === 'right' ? ' num' : '') +
        (c.wrap ? ' wrap' : ''), text: c.title });
    })));
    var tbody = h('tbody');
    if (!rows.length) {
      tbody.appendChild(h('tr', { class: 'empty-row' },
        h('td', { colspan: columns.length }, opts.empty || '暂无数据')));
    } else {
      rows.forEach(function (row, idx) {
        var tr = h('tr', null, columns.map(function (c) {
          var td = h('td', { class: (c.className || '') + (c.align === 'right' ? ' num' : '') + (c.wrap ? ' wrap' : '') });
          var val;
          if (typeof c.render === 'function') val = c.render(row, idx);
          else val = row[c.key];
          if (val instanceof global.Node) td.appendChild(val);
          else if (typeof val === 'string') td.innerHTML = val;
          else append(td, val);
          return td;
        }));
        tbody.appendChild(tr);
      });
    }
    return h('div', { class: 'table-wrap' }, h('table', { class: 'data' }, thead, tbody));
  }

  /* ---------------- 图表（纯 CSS） ---------------- */
  function trendBars(points, opts) {
    opts = opts || {};
    var max = Math.max(1, Math.max.apply(null, points.map(function (p) { return p.count; })));
    var todayKey = opts.todayKey;
    return h('div', { class: 'trend' }, points.map(function (p) {
      var pct = Math.round((p.count / max) * 100);
      return h('div', {
        class: 'bar-col' + (p.count === 0 ? ' is-zero' : '') + (todayKey && p.date === todayKey ? ' is-today' : ''),
        title: p.date + '：' + p.count + ' 单'
      },
        h('div', { class: 'bar-value', text: String(p.count) }),
        h('div', { class: 'bar', style: { height: Math.max(4, pct) + '%' } }),
        h('div', { class: 'bar-label', text: p.label })
      );
    }));
  }

  /** 横向占比条：items = [{label, value, tone}] */
  function distBars(items) {
    var max = Math.max(1, Math.max.apply(null, items.map(function (i) { return i.value; })));
    return h('div', { class: 'dist' }, items.map(function (i) {
      return h('div', { class: 'dist-row' },
        h('span', { class: 'dist-label', text: i.label }),
        h('div', { class: 'dist-track' },
          h('div', { class: 'dist-fill', style: { width: Math.round((i.value / max) * 100) + '%', background: i.color || '' } })),
        h('span', { class: 'dist-num', text: String(i.value) })
      );
    }));
  }

  /* ---------------- 抽屉 ---------------- */
  function drawer(opts) {
    opts = opts || {};
    var titleEl = h('span', { text: opts.title || '详情' });
    var bodyEl = h('div', { class: 'drawer-body' });
    var mask = h('div', { class: 'drawer-mask' });
    var panel = h('div', { class: 'drawer' },
      h('div', { class: 'drawer-header' }, titleEl,
        h('button', { class: 'close', text: '×', onclick: close })),
      bodyEl
    );
    doc.body.appendChild(mask);
    doc.body.appendChild(panel);
    mask.addEventListener('click', close);

    function open() {
      // 触发过渡：先插入 DOM 再加 class
      global.requestAnimationFrame(function () {
        mask.classList.add('show');
        panel.classList.add('show');
      });
    }
    function close() {
      mask.classList.remove('show');
      panel.classList.remove('show');
      setTimeout(function () {
        if (mask.parentNode) mask.parentNode.removeChild(mask);
        if (panel.parentNode) panel.parentNode.removeChild(panel);
        if (opts.onClose) opts.onClose();
      }, 250);
    }
    if (opts.body) bodyEl.appendChild(opts.body);
    if (opts.open !== false) open();

    return {
      open: open,
      close: close,
      setTitle: function (t) { titleEl.textContent = t; },
      setBody: function (node) { bodyEl.innerHTML = ''; if (node) bodyEl.appendChild(node); },
      bodyEl: bodyEl,
      panelEl: panel
    };
  }

  /** 通用弹窗；actions = [{text, tone, onClick(close), primary}] */
  function modal(opts) {
    opts = opts || {};
    var mask = h('div', { class: 'modal-mask' });
    var actions = (opts.actions || []).map(function (a) {
      return h('button', {
        class: 'btn' + (a.tone ? ' ' + a.tone : (a.primary ? ' btn-primary' : '')),
        text: a.text,
        onclick: function () { if (a.onClick) a.onClick(close); else close(); }
      });
    });
    var box = h('div', { class: 'modal' },
      opts.title ? h('h3', { text: opts.title }) : null,
      opts.body || null,
      actions.length ? h('div', { class: 'modal-actions' }, actions) : null
    );
    mask.appendChild(box);
    mask.addEventListener('click', function (e) { if (e.target === mask) close(); });
    doc.body.appendChild(mask);

    function close() {
      if (mask.parentNode) mask.parentNode.removeChild(mask);
      if (opts.onClose) opts.onClose();
    }
    return { close: close, boxEl: box };
  }

  function confirm(opts) {
    opts = opts || {};
    return new Promise(function (resolve) {
      var m = modal({
        title: opts.title || '请确认',
        body: h('div', { style: { fontSize: '13.5px', color: '#1C2420', lineHeight: '1.7' }, text: opts.message || '' }),
        actions: [
          { text: '取消', onClick: function (close) { close(); resolve(false); } },
          { text: opts.confirmText || '确定', primary: true, onClick: function (close) { close(); resolve(true); } }
        ],
        onClose: function () { resolve(false); }
      });
      return m;
    });
  }

  /* ---------------- 提示 / 空态 / 占位 ---------------- */
  function toast(message, type) {
    var host = doc.getElementById('toast-host');
    if (!host) return;
    var node = h('div', { class: 'toast' + (type ? ' ' + type : ''), text: message });
    host.appendChild(node);
    global.requestAnimationFrame(function () { node.classList.add('show'); });
    setTimeout(function () {
      node.classList.remove('show');
      setTimeout(function () { if (node.parentNode) node.parentNode.removeChild(node); }, 250);
    }, 2600);
  }

  function empty(text, icon) {
    return h('div', { class: 'empty' },
      h('span', { class: 'empty-icon', text: icon || '' }),
      text || '暂无数据'
    );
  }

  function placeholder(title, message) {
    return h('div', { class: 'placeholder-view' },
      h('div', { class: 'ph-title', text: title }),
      h('div', { text: message || '该视图正在开发中' })
    );
  }

  function skeletonCards(n) {
    var wrap = h('div', { class: 'grid grid-metrics' });
    for (var i = 0; i < (n || 4); i++) wrap.appendChild(h('div', { class: 'skeleton skeleton-card' }));
    return wrap;
  }
  function skeletonLines(n) {
    var wrap = h('div', null);
    for (var i = 0; i < (n || 4); i++) wrap.appendChild(h('div', { class: 'skeleton skeleton-line' }));
    return wrap;
  }

  /* ---------------- 列表能力：搜索 / 分页 ---------------- */
  /** 关键词过滤（多字段、忽略大小写） */
  function filterBy(items, keyword, fields) {
    var kw = (keyword || '').trim().toLowerCase();
    if (!kw) return items || [];
    return (items || []).filter(function (item) {
      return (fields || []).some(function (f) {
        var v = item[f];
        return v !== null && v !== undefined && String(v).toLowerCase().indexOf(kw) >= 0;
      });
    });
  }

  function paginate(items, page, size) {
    var total = (items || []).length;
    var pages = Math.max(1, Math.ceil(total / size));
    var p = Math.min(Math.max(1, page || 1), pages);
    var start = (p - 1) * size;
    return { items: (items || []).slice(start, start + size), total: total, page: p, pages: pages };
  }

  /** 分页控件：{page, pages, total, onChange(page)} */
  function pagination(opts) {
    var page = opts.page, pages = opts.pages;
    return h('div', { class: 'pagination' },
      h('span', { text: '共 ' + opts.total + ' 条' }),
      h('button', {
        class: 'btn btn-sm', text: '上一页', disabled: page <= 1,
        onclick: function () { if (page > 1) opts.onChange(page - 1); }
      }),
      h('span', { class: 'page-info', text: page + ' / ' + pages }),
      h('button', {
        class: 'btn btn-sm', text: '下一页', disabled: page >= pages,
        onclick: function () { if (page < pages) opts.onChange(page + 1); }
      })
    );
  }

  /** 搜索框：{placeholder, value, onInput(value)} */
  function searchBox(opts) {
    var input = h('input', { class: 'input input-sm', type: 'search', placeholder: opts.placeholder || '搜索…' });
    if (opts.value) input.value = opts.value;
    var timer = null;
    input.addEventListener('input', function () {
      clearTimeout(timer);
      timer = setTimeout(function () { opts.onInput(input.value); }, 200);   // 防抖
    });
    return h('div', { class: 'search-box' }, input);
  }

  /**
   * 列表区块（卡片 + 工具栏搜索 + 表格 + 分页），供各视图复用。
   * opts = {
   *   title, hint, extra,            // 卡片头
   *   rows, columns, empty,          // 数据与列定义
   *   searchPlaceholder, searchFields,   // 关键词过滤字段
   *   controls,                      // 额外的筛选控件节点（如状态下拉）
   *   pageSize
   * }
   * 返回 { node, redraw }
   */
  function listSection(opts) {
    var state = { keyword: '', page: 1 };
    var pageSize = opts.pageSize || 10;
    var tableHost = h('div');
    var pagerHost = h('div');
    var countHint = h('span', { class: 'count-hint' });

    function draw() {
      var filtered = filterBy(opts.rows, state.keyword, opts.searchFields || []);
      var page = paginate(filtered, state.page, pageSize);
      state.page = page.page;
      tableHost.innerHTML = '';
      tableHost.appendChild(dataTable({ columns: opts.columns, rows: page.items, empty: opts.empty }));
      pagerHost.innerHTML = '';
      if (page.pages > 1) {
        pagerHost.appendChild(pagination({
          page: page.page, pages: page.pages, total: page.total,
          onChange: function (p) { state.page = p; draw(); }
        }));
      }
      countHint.textContent = opts.rows && opts.rows.length !== filtered.length
        ? '（' + filtered.length + ' / ' + opts.rows.length + '）'
        : '（' + (opts.rows ? opts.rows.length : 0) + '）';
    }

    var toolbar = h('div', { class: 'toolbar' },
      opts.searchPlaceholder
        ? searchBox({
          placeholder: opts.searchPlaceholder,
          onInput: function (v) { state.keyword = v; state.page = 1; draw(); }
        })
        : null,
      opts.controls || null,
      h('span', { class: 'spacer' }),
      countHint
    );

    var node = card({
      title: opts.title,
      hint: opts.hint,
      extra: opts.extra,
      body: h('div', null, toolbar, tableHost, pagerHost)
    });

    draw();
    return { node: node, redraw: draw };
  }

  /**
   * 占位视图工厂：批次 3 迁移期间，未实现的菜单项先占位（避免点击空白/报错）。
   * cfg = { path, title, breadcrumb, note }
   */
  function placeholderRoute(cfg) {
    return {
      path: cfg.path,
      title: cfg.title,
      breadcrumb: cfg.breadcrumb || [cfg.title],
      async render(el) {
        el.appendChild(h('div', { class: 'page-head' },
          h('h2', { text: cfg.title }),
          h('span', { class: 'sub', text: '待迁移' })
        ));
        el.appendChild(placeholder(cfg.title, cfg.note || '该视图将在后续批次迁移，功能与旧版保持一致。'));
      }
    };
  }

  /* ---------------- 工单动作（服务端 allowedActions 下发，前端不再硬编码状态判断） ---------------- */
  var ACTION_LABEL = {
    VIEW: '查看', ACCEPT: '接单', REPAIR: '提交维修记录', PRICE: '核价', SIGN: '签字验收'
  };
  function actionLabel(code) { return ACTION_LABEL[code] || code; }
  function can(actions, code) { return Array.isArray(actions) && actions.indexOf(code) >= 0; }

  /* ---------------- 签字方式（审计留痕） ---------------- */
  var SIGN_TYPE_LABEL = { HANDWRITTEN: '手写签名', TYPED: '输入姓名生成' };

  /** 签字方式徽标：TYPED 用 warn 色提醒是便捷模式；历史数据（NULL）显示「未记录」 */
  function signTypeBadge(code) {
    var label = SIGN_TYPE_LABEL[code];
    if (!label) return badge('未记录', 'muted');
    return badge(label, code === 'TYPED' ? 'warn' : 'primary');
  }

  Admin.ui = {
    esc: esc, h: h, append: append,
    fmtTime: fmtTime, money: money, fmtDate: fmtDate,
    badge: badge, statusBadge: statusBadge, signTypeBadge: signTypeBadge,
    actionLabel: actionLabel, can: can, ACTION_LABEL: ACTION_LABEL,
    card: card, metricCard: metricCard, dataTable: dataTable,
    trendBars: trendBars, distBars: distBars,
    drawer: drawer, modal: modal, confirm: confirm,
    toast: toast, empty: empty, placeholder: placeholder, placeholderRoute: placeholderRoute,
    skeletonCards: skeletonCards, skeletonLines: skeletonLines,
    filterBy: filterBy, paginate: paginate, pagination: pagination, searchBox: searchBox,
    listSection: listSection,
    /** 状态字典：{CODE:{label,tone}} */
    STATUS: {
      REPORT: {
        PENDING: { label: '待受理', tone: 'pending' },
        DISPATCHED: { label: '已派单', tone: 'dispatched' },
        COMPLETED: { label: '已完成', tone: 'success' },
        REIMBURSED: { label: '已报销', tone: 'success' },
        CANCELLED: { label: '已取消', tone: 'cancelled' }
      },
      WORK_ORDER: {
        ASSIGNED: { label: '待接单', tone: 'primary' },
        IN_PROGRESS: { label: '维修中', tone: 'warn' },
        PENDING_PRICING: { label: '待核价', tone: 'warn' },
        PENDING_SIGN: { label: '待签字', tone: 'warn' },
        PENDING_REIMBURSE: { label: '待报销', tone: 'success' },
        REIMBURSED: { label: '已报销', tone: 'success' }
      },
      REIMBURSEMENT: {
        PENDING_SIGN: { label: '待签字', tone: 'pending' },
        PENDING_REIMBURSE: { label: '待报销', tone: 'primary' },
        REIMBURSED: { label: '已报销', tone: 'success' }
      }
    }
  };
})(window);
