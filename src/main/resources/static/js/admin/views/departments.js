/*!
 * 视图：系统与基础数据配置 · 科室索引管理（#/departments）
 * 功能：按「院区 → 楼栋 → 楼层」分类浏览科室（复用 DeptCascade.groupByLocation）+ 关键词搜索
 * 说明：科室 68 个来自医院真实楼层索引；floor 由后端字段提供（见 findings D-004）
 */
(function (global) {
  'use strict';
  var Admin = global.Admin = global.Admin || {};
  Admin.views = Admin.views || {};

  var state = { depts: [], keyword: '' };

  async function load(force) {
    state.depts = await Admin.store.loadDepartments(force) || [];
  }

  /** 过滤后重建分组（关键词命中科室名即保留该科室，空组自动消失） */
  function filteredGroups() {
    var kw = state.keyword.trim().toLowerCase();
    var list = state.depts.filter(function (d) {
      if (!kw) return true;
      var hit = String(d.name || '').toLowerCase().indexOf(kw) >= 0 ||
        String(d.departmentHead || '').toLowerCase().indexOf(kw) >= 0;
      return hit;
    });
    return { groups: DeptCascade.groupByLocation(list), count: list.length };
  }

  function groupNode(g) {
    var ui = Admin.ui;
    return ui.h('div', { class: 'dept-campus' },
      ui.h('div', { class: 'dept-campus-head' },
        ui.h('span', { text: g.label }),
        ui.h('span', { class: 'c', text: g.count + ' 个科室' })
      ),
      g.buildings.map(function (b) {
        return ui.h('div', { class: 'dept-building' },
          ui.h('div', { class: 'dept-building-name', text: b.building + '（' + b.count + '）' }),
          b.floors.map(function (f) {
            return ui.h('div', { class: 'dept-floor-row' },
              ui.h('span', { class: 'floor-chip', text: f.floor }),
              ui.h('div', { class: 'dept-names' }, f.departments.map(function (d) {
                return ui.h('span', {
                  class: 'dept-name-chip',
                  title: d.name + (d.departmentHead ? (' · 负责人：' + d.departmentHead) : ''),
                  text: DeptCascade.shortNameOf(d)
                });
              }))
            );
          })
        );
      })
    );
  }

  function paint(el) {
    var ui = Admin.ui;
    var res = filteredGroups();
    var groups = res.groups;

    var host = el.querySelector('#dept-body');
    host.innerHTML = '';

    host.appendChild(ui.h('div', { class: 'grid grid-metrics', style: { marginBottom: '16px' } },
      ui.metricCard({
        title: '科室总数', value: state.depts.length, unit: '个',
        icon: Admin.layout.ICONS.building, hint: '来源：医院科室楼层索引'
      }),
      ui.metricCard({
        title: '院区', value: groups.length, unit: '个',
        icon: Admin.layout.ICONS.dashboard, hint: groups.map(function (g) { return g.label; }).join(' · ') || '-'
      }),
      ui.metricCard({
        title: '楼栋', value: groups.reduce(function (n, g) { return n + g.buildings.length; }, 0), unit: '栋',
        icon: Admin.layout.ICONS.clipboard, hint: '覆盖门诊/病房/住院部/急救中心'
      }),
      ui.metricCard({
        title: '当前筛选结果', value: res.count, unit: '个',
        icon: Admin.layout.ICONS.users, tone: state.keyword ? 'warn' : '', hint: state.keyword ? ('关键词：' + state.keyword) : '未筛选'
      })
    ));

    host.appendChild(ui.card({
      title: '科室索引',
      hint: '（' + res.count + ' / ' + state.depts.length + '）',
      extra: ui.h('div', { style: { display: 'flex', gap: '8px', alignItems: 'center' } },
        ui.searchBox({
          placeholder: '搜索科室名称 / 负责人',
          onInput: function (v) { state.keyword = v; paint(el); }
        }),
        ui.h('button', {
          class: 'btn btn-ghost btn-sm', text: '刷新',
          onclick: async function () { await load(true); paint(el); }
        })
      ),
      body: groups.length
        ? ui.h('div', null, groups.map(groupNode))
        : ui.empty(state.keyword ? '没有匹配的科室' : '暂无科室数据')
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
      paint(el);
    }
  };
})(window);
