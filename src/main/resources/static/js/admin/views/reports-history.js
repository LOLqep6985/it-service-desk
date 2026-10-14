/*!
 * 视图：工单业务 · 报修单历史（#/reports/history）
 * 功能：已受理/已派单/已取消的报修单列表（状态过滤 + 搜索 + 分页）
 * 说明：待受理（PENDING）不在本页，见「待受理报修单」
 */
(function (global) {
  'use strict';
  var Admin = global.Admin = global.Admin || {};
  Admin.views = Admin.views || {};

  var state = { rows: [], status: '' };

  var STATUS_OPTIONS = [
    { value: '', label: '全部（非待受理）' },
    { value: 'DISPATCHED', label: '已派单' },
    { value: 'ACCEPTED', label: '已受理' },
    { value: 'CANCELLED', label: '已取消' }
  ];

  async function load() {
    var all = await Admin.api.get('/api/reports') || [];
    if (state.status) {
      state.rows = all.filter(function (r) { return r.status === state.status; });
    } else {
      state.rows = all.filter(function (r) { return r.status !== 'PENDING'; });
    }
  }

  async function paint(el) {
    var ui = Admin.ui;
    await load();

    var statusSelect = ui.h('select', {
      class: 'input input-sm', style: { width: '160px' },
      onchange: function (e) { state.status = e.target.value; refresh(el); }
    }, STATUS_OPTIONS.map(function (o) {
      return ui.h('option', { value: o.value, text: o.label, selected: o.value === state.status });
    }));

    var list = ui.listSection({
      title: '报修单历史',
      hint: '（已流转的报修单）',
      searchPlaceholder: '搜索报修单号 / 科室 / 设备 / 报修人',
      searchFields: ['reportNo', 'departmentName', 'deviceName', 'reporterName', 'departmentHead', 'problemDesc'],
      rows: state.rows,
      empty: '暂无历史报修单',
      pageSize: 12,
      controls: statusSelect,
      extra: ui.h('button', {
        class: 'btn btn-ghost btn-sm', text: '刷新',
        onclick: function () { refresh(el); }
      }),
      columns: [
        { key: 'reportNo', title: '报修单号', render: function (r) { return '<span class="cell-strong">' + ui.esc(r.reportNo || r.id) + '</span>'; } },
        { key: 'departmentName', title: '科室', wrap: true },
        { key: 'deviceName', title: '设备' },
        { key: 'reporterName', title: '报修人' },
        { key: 'problemDesc', title: '描述', wrap: true },
        { key: 'departmentHead', title: '科室负责人' },
        { key: 'status', title: '状态', render: function (r) { return ui.statusBadge(ui.STATUS.REPORT, r.status); } },
        { key: 'createdAt', title: '提交时间', render: function (r) { return ui.fmtTime(r.createdAt); } }
      ]
    });

    var host = el.querySelector('#history-body');
    host.innerHTML = '';
    host.appendChild(list.node);
  }

  async function refresh(el) {
    var host = el.querySelector('#history-body');
    host.innerHTML = '';
    host.appendChild(Admin.ui.skeletonLines(6));
    await paint(el);
  }

  Admin.views.reportsHistory = {
    path: '/reports/history',
    title: '报修单历史',
    breadcrumb: ['工单业务管理', '报修单历史'],

    async render(el) {
      var ui = Admin.ui;
      el.appendChild(ui.h('div', { class: 'page-head' },
        ui.h('h2', { text: '报修单历史' }),
        ui.h('span', { class: 'sub', text: '已派单 / 已受理 / 已取消的报修单留档' })
      ));
      el.appendChild(ui.h('div', { id: 'history-body' }, ui.skeletonLines(6)));
      await paint(el);
    }
  };
})(window);
