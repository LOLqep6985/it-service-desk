/*!
 * 视图：工单业务 · 待受理报修单（#/reports/pending）
 * 功能：待受理列表（搜索/分页）+ 派单（选择维修工 → 生成工单）
 */
(function (global) {
  'use strict';
  var Admin = global.Admin = global.Admin || {};
  Admin.views = Admin.views || {};

  var state = { rows: [], repairers: [], list: null };

  async function load() {
    var ui = Admin.ui;
    var results = await Promise.all([
      Admin.api.get('/api/reports', { status: 'PENDING' }),
      Admin.api.get('/api/users', { role: 'REPAIRER' })
    ]);
    state.rows = results[0] || [];
    state.repairers = results[1] || [];
  }

  function dispatchDialog(report, onDone) {
    var ui = Admin.ui;
    var select = ui.h('select', { class: 'input' }, state.repairers.map(function (u) {
      return ui.h('option', { value: u.id, text: u.name + '（' + (u.username || '') + '）' });
    }));

    var body = ui.h('div', null,
      ui.h('div', {
        style: { background: '#f8fafc', borderRadius: '8px', padding: '10px 12px', fontSize: '13px', marginBottom: '12px', lineHeight: '1.7' }
      },
        ui.h('div', null, ui.h('b', { text: report.reportNo || ('#' + report.id) }) + ' · ' + (report.departmentName || '')),
        ui.h('div', { style: { color: '#7f8c8d' } }, '设备：' + (report.deviceName || '') + ' · 报修人：' + (report.reporterName || '')),
        report.problemDesc ? ui.h('div', { style: { color: '#7f8c8d' } }, '描述：' + report.problemDesc) : null
      ),
      state.repairers.length
        ? ui.h('div', { class: 'field' }, ui.h('label', { text: '指派维修人员' }), select)
        : ui.h('div', { class: 'empty', text: '没有可用的维修人员，请先在用户管理中维护' })
    );

    var m = Admin.ui.modal({
      title: '派单给维修人员',
      body: body,
      actions: state.repairers.length ? [
        { text: '取消' },
        {
          text: '确认派单', primary: true,
          onClick: async function (close) {
            var assigneeId = select.value;
            close();
            try {
              await Admin.api.post('/api/reports/' + report.id + '/dispatch', null, { assigneeId: assigneeId });
              Admin.ui.toast('已派单，工单已生成', 'success');
              await onDone();
            } catch (e) {
              Admin.ui.toast(e.message, 'error');
            }
          }
        }
      ] : [{ text: '知道了', primary: true }]
    });
    return m;
  }

  async function renderBody(el) {
    var ui = Admin.ui;
    await load();

    var list = ui.listSection({
      title: '待受理报修单',
      hint: '（待受理）',
      searchPlaceholder: '搜索报修单号 / 科室 / 设备 / 报修人',
      searchFields: ['reportNo', 'departmentName', 'deviceName', 'reporterName', 'problemDesc'],
      rows: state.rows,
      empty: '暂无待受理报修单',
      pageSize: 10,
      extra: ui.h('button', {
        class: 'btn btn-ghost btn-sm', text: '刷新',
        onclick: function () { refreshAll(el); }
      }),
      columns: [
        { key: 'reportNo', title: '报修单号', render: function (r) { return '<span class="cell-strong">' + ui.esc(r.reportNo || r.id) + '</span>'; } },
        { key: 'departmentName', title: '科室', wrap: true },
        { key: 'deviceName', title: '设备' },
        { key: 'reporterName', title: '报修人' },
        { key: 'problemDesc', title: '描述', wrap: true },
        { key: 'departmentHead', title: '科室负责人' },
        { key: 'createdAt', title: '提交时间', render: function (r) { return ui.fmtTime(r.createdAt); } },
        {
          title: '操作', render: function (r) {
            return ui.h('button', {
              class: 'btn btn-primary btn-sm', text: '派单',
              onclick: function () {
                dispatchDialog(r, async function () {
                  await refreshAll(el);
                });
              }
            });
          }
        }
      ]
    });
    state.list = list;
    return list.node;
  }

  function statRow(el) {
    var ui = Admin.ui;
    return ui.h('div', { class: 'grid grid-metrics' },
      ui.metricCard({
        title: '待受理报修单', value: state.rows.length, unit: '单',
        icon: Admin.layout.ICONS.clipboard, tone: 'danger', hint: '需尽快派单'
      }),
      ui.metricCard({
        title: '可用维修人员', value: state.repairers.length, unit: '人',
        icon: Admin.layout.ICONS.users, hint: state.repairers.map(function (u) { return u.name; }).join(' · ') || '无'
      })
    );
  }

  async function refreshAll(el) {
    var host = el.querySelector('#pending-body');
    host.innerHTML = '';
    host.appendChild(ui_skeleton());
    var body = await renderBody(el);
    host.innerHTML = '';
    host.appendChild(body);
    var statHost = el.querySelector('#pending-stat');
    statHost.innerHTML = '';
    statHost.appendChild(statRow(el));
    Admin.refreshBadges();
  }

  function ui_skeleton() { return Admin.ui.skeletonLines(5); }

  Admin.views.reportsPending = {
    path: '/reports/pending',
    title: '待受理报修单',
    breadcrumb: ['工单业务管理', '待受理报修单'],

    async render(el) {
      var ui = Admin.ui;
      el.appendChild(ui.h('div', { class: 'page-head' },
        ui.h('h2', { text: '待受理报修单' }),
        ui.h('span', { class: 'sub', text: '科室提交的报修单，派单后生成工单并进入维修人员今日任务' })
      ));
      el.appendChild(ui.h('div', { id: 'pending-stat' }, ui.skeletonCards(2)));
      el.appendChild(ui.h('div', { id: 'pending-body' }, ui.skeletonLines(5)));

      await refreshAll(el);
    }
  };
})(window);
