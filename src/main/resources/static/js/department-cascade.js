/*!
 * 科室三级联动组件（院区 → 楼栋 → 科室）
 * 使用方：report.html（科室免登录报修绑定）、create.html（登录报修）、admin.html（楼层标签）
 *
 * 数据约定：department.name 形如「门诊楼-一层-收费室」。
 * 为什么不在后端改 name：
 *   1) name 承担全局唯一键（uk_dept_name）——「药房」等重名靠「楼栋-楼层-」前缀区分；
 *   2) report.department_name / work_order.department_name 存的是名字快照，
 *      改 name 会导致历史记录与新记录展示不一致。
 * 因此 name 保持原样，楼层优先取后端 floor 字段（如 3F），展示时再拆出科室短名。
 */
(function (global) {
  'use strict';

  var CAMPUS_LABEL = { LAOYUAN: '老院区', XIYUAN: '西院区', DONGYUAN: '东区院区' };
  var FLOOR_CN = {
    '一层': '1F', '二层': '2F', '三层': '3F', '四层': '4F', '五层': '5F', '六层': '6F',
    '七层': '7F', '八层': '8F', '九层': '9F', '十层': '10F', '十一层': '11F', '十二层': '12F'
  };
  var ARROW = '<span class="arrow">▾</span>';

  function esc(s) {
    return (s == null ? '' : String(s)).replace(/[&<>"']/g, function (c) {
      return { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c];
    });
  }
  function nameParts(dept) { return String((dept && dept.name) || '').split('-'); }
  function buildingOf(dept) {
    if (dept && dept.building) return dept.building;
    var p = nameParts(dept);
    return p.length >= 3 ? p[0] : '';
  }
  /** 楼层号：优先用后端 floor 字段，缺失时按中文楼层兜底 */
  function floorOf(dept) {
    if (dept && dept.floor) return dept.floor;
    var p = nameParts(dept);
    return p.length >= 3 ? (FLOOR_CN[p[1]] || p[1] || '') : '';
  }
  /** 科室短名：去掉「楼栋-楼层-」前缀 */
  function shortNameOf(dept) {
    var p = nameParts(dept);
    return p.length >= 3 ? p.slice(2).join('-') : ((dept && dept.name) || '');
  }
  function floorChip(dept) {
    var f = floorOf(dept);
    return f ? '<span class="floor-chip">' + esc(f) + '</span>' : '';
  }
  function campusLabel(code) { return CAMPUS_LABEL[code] || code || ''; }
  function floorNumber(f) {
    var m = /^(\d+)/.exec(String(f == null ? '' : f));
    return m ? parseInt(m[1], 10) : 999;
  }

  /**
   * 按 院区 → 楼栋 → 楼层 分组，供管理后台「基础数据」分类展示。
   * 返回 [{ campus, label, count, buildings: [{ building, count, floors: [{ floor, departments }] }] }]
   * 楼层按数字升序（1F..12F），科室保持后端返回顺序（id 升序）。
   */
  function groupByLocation(depts) {
    var map = {}, order = [];
    (depts || []).forEach(function (d) {
      var ck = d.campus || '';
      if (!map[ck]) { map[ck] = { campus: ck, label: campusLabel(ck), count: 0, buildings: [], _b: {} }; order.push(ck); }
      var c = map[ck];
      c.count++;
      var bk = buildingOf(d);
      if (!c._b[bk]) { c._b[bk] = { building: bk, count: 0, floors: [], _f: {} }; c.buildings.push(c._b[bk]); }
      var b = c._b[bk];
      b.count++;
      var f = floorOf(d) || '—';
      if (!b._f[f]) { b._f[f] = { floor: f, departments: [] }; b.floors.push(b._f[f]); }
      b._f[f].departments.push(d);
    });
    order.forEach(function (ck) {
      map[ck].buildings.forEach(function (b) {
        b.floors.sort(function (x, y) { return floorNumber(x.floor) - floorNumber(y.floor); });
      });
    });
    return order.map(function (ck) { return map[ck]; });
  }

  /**
   * 绑定三级联动到页面元素。
   * opts: {
   *   campusEl, buildingEl, valueEl, panelEl, wrapEl,   // 必填的 DOM 元素
   *   onSelect(dept|null),                              // 选中/清空回调
   *   onChange()                                        // 任一级别变化回调（用于清提示文案）
   * }
   */
  function create(opts) {
    var campusEl = opts.campusEl, buildingEl = opts.buildingEl;
    var valueEl = opts.valueEl, panelEl = opts.panelEl, wrapEl = opts.wrapEl;
    var depts = [], selected = null;

    function notifySelect() { if (opts.onSelect) opts.onSelect(selected); }
    function notifyChange() { if (opts.onChange) opts.onChange(); }

    function renderCampus() {
      var seen = [], out = ['<option value="">请选择院区</option>'];
      depts.forEach(function (d) {
        if (d.campus && seen.indexOf(d.campus) < 0) {
          seen.push(d.campus);
          out.push('<option value="' + esc(d.campus) + '">' + esc(campusLabel(d.campus)) + '</option>');
        }
      });
      if (campusEl) { campusEl.innerHTML = out.join(''); campusEl.value = ''; }
    }

    /** 第二级：该院区下的楼栋（院区变化时重建并清空） */
    function renderBuilding(campus) {
      var buildings = [];
      if (campus) {
        depts.forEach(function (d) {
          if (d.campus === campus) {
            var b = buildingOf(d);
            if (b && buildings.indexOf(b) < 0) buildings.push(b);
          }
        });
      }
      if (buildingEl) {
        buildingEl.innerHTML = buildings.length
          ? '<option value="">请选择楼栋</option>' + buildings.map(function (b) {
            return '<option value="' + esc(b) + '">' + esc(b) + '</option>';
          }).join('')
          : '<option value="">请先选择院区</option>';
        buildingEl.disabled = buildings.length === 0;
        buildingEl.value = '';
      }
    }

    /** 第三级：该楼栋下的科室（楼层标签 + 科室名），楼栋变化时重建并清空 */
    function renderDept(campus, building) {
      selected = null;
      if (panelEl) panelEl.classList.remove('show');
      if (!campus || !building) {
        if (valueEl) { valueEl.disabled = true; valueEl.innerHTML = '请先选择楼栋' + ARROW; }
        if (panelEl) panelEl.innerHTML = '';
        notifySelect();
        return;
      }
      var list = depts.filter(function (d) { return d.campus === campus && buildingOf(d) === building; });
      if (!list.length) {
        if (valueEl) { valueEl.disabled = true; valueEl.innerHTML = '该楼栋暂无科室' + ARROW; }
        if (panelEl) panelEl.innerHTML = '<div class="picker-empty">该楼栋暂无科室</div>';
        notifySelect();
        return;
      }
      if (valueEl) {
        valueEl.disabled = false;
        valueEl.innerHTML = '请选择科室（共 ' + list.length + ' 个）' + ARROW;
      }
      if (panelEl) {
        panelEl.innerHTML = list.map(function (d) {
          return '<div class="picker-opt" data-id="' + d.id + '">' + floorChip(d) +
            '<span class="opt-name">' + esc(shortNameOf(d)) + '</span></div>';
        }).join('');
      }
      notifySelect();
    }

    function pick(id) {
      var dept = null;
      for (var i = 0; i < depts.length; i++) {
        if (String(depts[i].id) === String(id)) { dept = depts[i]; break; }
      }
      if (!dept) return null;
      selected = dept;
      if (valueEl) {
        valueEl.innerHTML = floorChip(dept) +
          '<span class="opt-name">' + esc(shortNameOf(dept)) + '</span>' + ARROW;
      }
      if (panelEl) {
        var nodes = panelEl.querySelectorAll('.picker-opt');
        for (var j = 0; j < nodes.length; j++) {
          var on = String(nodes[j].getAttribute('data-id')) === String(dept.id);
          if (on) { nodes[j].classList.add('active'); } else { nodes[j].classList.remove('active'); }
        }
        panelEl.classList.remove('show');
      }
      notifySelect();
      return dept;
    }

    // 第一级变化：清空第二、三级，并动态加载该院区下的楼栋
    if (campusEl) {
      campusEl.addEventListener('change', function () {
        renderBuilding(campusEl.value);
        renderDept(campusEl.value, '');
        notifyChange();
      });
    }
    // 第二级变化：清空第三级，并动态加载该楼栋下的科室
    if (buildingEl) {
      buildingEl.addEventListener('change', function () {
        renderDept(campusEl ? campusEl.value : '', buildingEl.value);
        notifyChange();
      });
    }
    if (valueEl) {
      valueEl.addEventListener('click', function () {
        if (valueEl.disabled || !panelEl) return;
        panelEl.classList.toggle('show');
      });
    }
    if (panelEl) {
      panelEl.addEventListener('click', function (e) {
        var opt = e.target && e.target.closest ? e.target.closest('.picker-opt') : null;
        if (opt) pick(opt.getAttribute('data-id'));
      });
    }
    if (global.document && wrapEl) {
      global.document.addEventListener('click', function (e) {
        if (e.target && wrapEl.contains(e.target)) return;
        if (panelEl) panelEl.classList.remove('show');
      });
    }

    return {
      /** 注入科室数据并复位三级 */
      setDepartments: function (list) {
        depts = list || [];
        renderCampus();
        renderBuilding('');
        renderDept('', '');
      },
      renderCampus: renderCampus,
      renderBuilding: renderBuilding,
      renderDept: renderDept,
      pick: pick,
      selected: function () { return selected; },
      byId: function (id) {
        for (var i = 0; i < depts.length; i++) { if (String(depts[i].id) === String(id)) return depts[i]; }
        return null;
      }
    };
  }

  global.DeptCascade = {
    esc: esc,
    campusLabel: campusLabel,
    buildingOf: buildingOf,
    floorOf: floorOf,
    shortNameOf: shortNameOf,
    floorChip: floorChip,
    groupByLocation: groupByLocation,
    create: create
  };
})(window);
