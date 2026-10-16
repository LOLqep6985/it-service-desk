/*!
 * 视图：系统与基础数据配置 · 科室索引管理（#/departments）
 * 功能：按「院区 → 楼栋 → 楼层」分类浏览科室（复用 DeptCascade.groupByLocation）
 * 说明：科室 68 个来自医院真实楼层索引；floor 由后端字段提供（见 findings D-004）
 * 布局：院区三栏卡片，楼栋分区，楼层行内科室 chip 横向流式排列；
 *       分类结构本身已一目了然，不设搜索（用户裁定：搜索冗余，见 findings D-017）
 */
(function (global) {
  'use strict';
  var Admin = global.Admin = global.Admin || {};
  Admin.views = Admin.views || {};

  var state = { depts: [] };

  async function load(force) {
    state.depts = await Admin.store.loadDepartments(force) || [];
  }

  function groups() {
    return window.DeptCascade.groupByLocation(state.depts);
  }

  /** 楼栋节点：楼栋名 + 楼层行（floor-chip + 科室 chips 横排） */
  function buildingNode(b) {
    var ui = Admin.ui;
    return ui.h('div', { class: 'dept-building' },
      ui.h('div', { class: 'dept-building-name' },
        ui.h('span', { class: 'dept-building-title', text: b.building }),
        ui.h('span', { class: 'dept-building-count', text: b.count + ' 科室' })
      ),
      b.floors.map(function (f) {
        return ui.h('div', { class: 'dept-floor-row' },
          ui.h('span', { class: 'floor-chip', text: f.floor }),
          ui.h('div', { class: 'dept-names' }, f.departments.map(function (d) {
            return ui.h('span', {
              class: 'dept-name-chip',
              title: d.name + (d.departmentHead ? (' · 负责人：' + d.departmentHead) : ''),
              text: window.DeptCascade.shortNameOf(d)
            });
          }))
        );
      })
    );
  }

  /** 院区节点：一张院区卡片，内含楼栋分区 */
  function groupNode(g) {
    var ui = Admin.ui;
    return ui.h('div', { class: 'dept-campus' },
      ui.h('div', { class: 'dept-campus-head' },
        ui.h('span', { class: 'dept-campus-title', text: g.label }),
        ui.h('span', { class: 'c', text: g.count + ' 个科室' })
      ),
      g.buildings.map(buildingNode)
    );
  }

  /** 重绘数据区（指标卡 + 分组内容） */
  function paintBody(el) {
    var ui = Admin.ui;
    var gs = groups();
    var host = el.querySelector('#dept-content');
    if (!host) return;
    host.innerHTML = '';

    // 同步卡片头计数（工具栏卡片只创建一次，hint 需手动刷新）
    var hintEl = el.querySelector('.card .count-hint');
    if (hintEl) hintEl.textContent = '（' + state.depts.length + ' / ' + state.depts.length + '）';

    host.appendChild(ui.h('div', { class: 'grid grid-metrics' },
      ui.metricCard({
        title: '科室总数', value: state.depts.length, unit: '个',
        icon: Admin.layout.ICONS.building, hint: '来源：医院科室楼层索引'
      }),
      ui.metricCard({
        title: '院区', value: gs.length, unit: '个',
        icon: Admin.layout.ICONS.dashboard, hint: gs.map(function (g) { return g.label; }).join(' · ') || '-'
      }),
      ui.metricCard({
        title: '楼栋', value: gs.reduce(function (n, g) { return n + g.buildings.length; }, 0), unit: '栋',
        icon: Admin.layout.ICONS.clipboard, hint: '覆盖门诊/病房/住院部/急救中心'
      }),
      ui.metricCard({
        title: '当前展示', value: state.depts.length, unit: '个',
        icon: Admin.layout.ICONS.users, hint: '全部科室'
      })
    ));

    host.appendChild(ui.card({
      title: '科室索引',
      hint: '（' + state.depts.length + ' / ' + state.depts.length + '）',
      body: gs.length
        ? ui.h('div', { class: 'dept-campus-grid' }, gs.map(groupNode))
        : ui.empty('暂无科室数据')
    }));
  }

  Admin.views.departments = {
    path: '/departments',
    title: '科室索引管理',
    breadcrumb: ['系统与基础数据配置', '科室索引管理'],

    async render(el) {
      var ui = Admin.ui;
      el.appendChild(ui.h('div', { class: 'page-head' },
        ui.h('h2', { text: '科室索引管理' }),
        ui.h('span', { class: 'sub', text: '按院区 → 楼栋 → 楼层分类查看；科室端三级联动使用同一份数据' })
      ));
      el.appendChild(ui.h('div', { id: 'dept-body' }, ui.skeletonLines(6)));
      await load(false);

      // 工具栏只有「刷新」；卡片骨架创建一次，数据区可独立重绘
      var host = el.querySelector('#dept-body');
      host.innerHTML = '';
      var toolbar = ui.card({
        title: '科室索引',
        hint: '（加载中）',
        extra: ui.h('button', {
          class: 'btn btn-ghost btn-sm', text: '刷新',
          onclick: async function () { await load(true); paintBody(el); }
        }),
        body: ui.h('div', { id: 'dept-content' })
      });
      host.appendChild(toolbar);
      paintBody(el);
    }
  };
})(window);
