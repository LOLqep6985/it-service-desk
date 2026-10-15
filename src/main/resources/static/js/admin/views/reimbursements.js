/*!
 * 视图：财务与报销 · 月度报销单（#/reimbursements）
 * 功能：按月生成报销单（汇总已签字工单的耗材金额）+ 列表（状态过滤）+ 详情抽屉 + 财务回填
 */
(function (global) {
  'use strict';
  var Admin = global.Admin = global.Admin || {};
  Admin.views = Admin.views || {};

  var state = { rows: [], status: '' };

  var STATUS_OPTIONS = [
    { value: '', label: '全部状态' },
    { value: 'PENDING_SIGN', label: '待签字' },
    { value: 'PENDING_REIMBURSE', label: '待报销' },
    { value: 'REIMBURSED', label: '已报销' }
  ];

  function currentMonth() {
    var d = new Date();
    return d.getFullYear() + '-' + String(d.getMonth() + 1).padStart(2, '0');
  }

  async function load() {
    state.rows = await Admin.api.get('/api/reimbursements', state.status ? { status: state.status } : null) || [];
  }

  async function generate(el, month) {
    var ui = Admin.ui;
    if (!month) { ui.toast('请选择月份', 'error'); return; }
    try {
      var created = await Admin.api.post('/api/reimbursements/generate', null, { month: month });
      var list = created || [];
      ui.toast(list.length ? ('已生成 ' + list.length + ' 个科室的报销单') : '该月份没有新的可报销工单（或已生成过）', list.length ? 'success' : 'warn');
      await refresh(el);
    } catch (e) {
      ui.toast(e.message, 'error');
    }
  }

  async function reimburse(el, row) {
    var ui = Admin.ui;
    var ok = await ui.confirm({
      title: '财务回填确认',
      message: '确认将 ' + row.month + ' · ' + (row.departmentName || '') + ' 的报销单（' + ui.money(row.totalAmount) + '）标记为「已报销」？',
      confirmText: '确认已报销'
    });
    if (!ok) return;
    try {
      await Admin.api.post('/api/reimbursements/' + row.id + '/reimburse');
      ui.toast('已标记为已报销', 'success');
      await refresh(el);
    } catch (e) {
      ui.toast(e.message, 'error');
    }
  }

  async function openDetail(row) {
    var ui = Admin.ui;
    var drawer = ui.drawer({ title: '报销单详情', body: ui.skeletonLines(6) });
    try {
      var d = await Admin.api.get('/api/reimbursements/' + row.id);
      var r = d.reimbursement || {};
      var wos = d.workOrders || [];
      var woTable = wos.length
        ? ui.h('div', { class: 'table-wrap' },
          ui.h('table', { class: 'data' },
            ui.h('thead', null, ui.h('tr', null,
              ui.h('th', { text: '工单' }), ui.h('th', { text: '标题' }),
              ui.h('th', { text: '签字人' }), ui.h('th', { text: '签字时间' }))),
            ui.h('tbody', null, wos.map(function (wo) {
              return ui.h('tr', null,
                ui.h('td', { html: '<span class="cell-strong">#' + ui.esc(wo.id) + '</span>' }),
                ui.h('td', { text: wo.title || '-' }),
                ui.h('td', { text: wo.signedBy || '-' }),
                ui.h('td', { text: wo.signedAt ? ui.fmtTime(wo.signedAt) : '-' })
              );
            }))
          )
        )
        : ui.empty('该报销单暂未关联工单');

      drawer.setBody(ui.h('div', null,
        ui.h('div', { class: 'detail-section' },
          ui.h('h4', { text: '报销单信息' }),
          detailRow('报销单ID', r.id),
          detailRow('月份', r.month),
          detailRow('科室', d.departmentName || '未填写'),
          detailRow('合计金额', ui.h('b', { text: ui.money(r.totalAmount) })),
          detailRow('状态', ui.statusBadge(ui.STATUS.REIMBURSEMENT, r.status)),
          detailRow('签字人', r.signedBy || '未签字'),
          detailRow('签字方式', ui.signTypeBadge(r.signType)),
          detailRow('签字时间', r.signedAt ? ui.fmtTime(r.signedAt) : '未签字'),
          r.reimbursedAt ? detailRow('报销时间', ui.fmtTime(r.reimbursedAt)) : null
        ),
        ui.h('div', { class: 'detail-section' },
          ui.h('h4', { text: '签字留痕' }),
          r.signatureUrl
            ? ui.h('div', { class: 'detail-photo' },
              ui.h('img', { src: r.signatureUrl, alt: '签名图', onclick: function (e) { global.open(e.target.src, '_blank'); } }))
            : ui.h('div', { style: { color: '#95a5a6', fontSize: '13px' }, text: '无签名图片（历史单据签字时未启用留痕）' })
        ),
        ui.h('div', { class: 'detail-section' },
          ui.h('h4', { text: '包含工单（' + wos.length + '）' }),
          woTable
        )
      ));
    } catch (e) {
      drawer.setBody(ui.empty('加载失败：' + e.message, '⚠️'));
    }
  }

  function detailRow(k, v) {
    var ui = Admin.ui;
    return ui.h('div', { class: 'detail-row' },
      ui.h('span', { class: 'k', text: k }),
      ui.h('span', { class: 'v' }, v instanceof global.Node ? v : String(v === null || v === undefined || v === '' ? '未填写' : v))
    );
  }

  async function paint(el) {
    var ui = Admin.ui;
    await load();

    var monthInput = ui.h('input', { class: 'input input-sm', type: 'month', value: currentMonth(), style: { width: '150px' } });
    var statusSelect = ui.h('select', {
      class: 'input input-sm', style: { width: '130px' },
      onchange: function (e) { state.status = e.target.value; refresh(el); }
    }, STATUS_OPTIONS.map(function (o) {
      return ui.h('option', { value: o.value, text: o.label, selected: o.value === state.status });
    }));

    var controls = ui.h('div', { style: { display: 'flex', gap: '8px', alignItems: 'center', flexWrap: 'wrap' } },
      monthInput,
      ui.h('button', {
        class: 'btn btn-primary btn-sm', text: '生成报销单',
        onclick: function () { generate(el, monthInput.value); }
      }),
      statusSelect
    );

    var list = ui.listSection({
      title: '月度报销单',
      hint: '（按已签字工单汇总耗材金额）',
      searchPlaceholder: '搜索月份 / 科室 / 签字人',
      searchFields: ['month', 'departmentName', 'signedBy'],
      rows: state.rows,
      empty: '暂无报销单，选择月份后点击「生成报销单」',
      pageSize: 10,
      controls: controls,
      extra: ui.h('button', {
        class: 'btn btn-ghost btn-sm', text: '刷新',
        onclick: function () { refresh(el); }
      }),
      columns: [
        { key: 'id', title: 'ID', render: function (r) { return '<span class="cell-strong">' + r.id + '</span>'; } },
        { key: 'month', title: '月份' },
        { key: 'departmentName', title: '科室', wrap: true, render: function (r) { return ui.esc(r.departmentName || r.departmentId || '-'); } },
        { key: 'totalAmount', title: '合计金额', align: 'right', render: function (r) { return '<b>' + ui.money(r.totalAmount) + '</b>'; } },
        { key: 'status', title: '状态', render: function (r) { return ui.statusBadge(ui.STATUS.REIMBURSEMENT, r.status); } },
        { key: 'signedBy', title: '签字人', render: function (r) { return ui.esc(r.signedBy || '-'); } },
        { key: 'signedAt', title: '签字时间', render: function (r) { return r.signedAt ? ui.fmtTime(r.signedAt) : '-'; } },
        {
          title: '操作', render: function (r) {
            var nodes = [ui.h('button', {
              class: 'btn btn-ghost btn-sm', text: '详情',
              onclick: function () { openDetail(r); }
            })];
            if (r.status === 'PENDING_REIMBURSE') {
              nodes.push(ui.h('button', {
                class: 'btn btn-success btn-sm', style: { marginLeft: '6px' }, text: '财务回填',
                onclick: function () { reimburse(el, r); }
              }));
            }
            return ui.h('div', null, nodes);
          }
        }
      ]
    });

    var host = el.querySelector('#reimb-body');
    host.innerHTML = '';
    host.appendChild(summaryCard());
    host.appendChild(list.node);
    Admin.refreshBadges();
  }

  function summaryCard() {
    var ui = Admin.ui;
    var total = state.rows.reduce(function (n, r) { return n + Number(r.totalAmount || 0); }, 0);
    var byStatus = { PENDING_SIGN: 0, PENDING_REIMBURSE: 0, REIMBURSED: 0 };
    state.rows.forEach(function (r) { if (byStatus[r.status] !== undefined) byStatus[r.status]++; });
    return ui.h('div', { class: 'grid grid-metrics', style: { marginBottom: '16px' } },
      ui.metricCard({ title: '列表合计金额', value: ui.money(total), icon: Admin.layout.ICONS.wallet, hint: '共 ' + state.rows.length + ' 张报销单' }),
      ui.metricCard({ title: '待签字', value: byStatus.PENDING_SIGN, unit: '张', icon: Admin.layout.ICONS.history, tone: 'warn', hint: '等待护士长签字' }),
      ui.metricCard({ title: '待报销', value: byStatus.PENDING_REIMBURSE, unit: '张', icon: Admin.layout.ICONS.clipboard, tone: 'danger', hint: '已签字，待财务回填' }),
      ui.metricCard({ title: '已报销', value: byStatus.REIMBURSED, unit: '张', icon: Admin.layout.ICONS.info, tone: 'success', hint: '流程闭环完成' })
    );
  }

  async function refresh(el) {
    var host = el.querySelector('#reimb-body');
    host.innerHTML = '';
    host.appendChild(Admin.ui.skeletonLines(6));
    await paint(el);
  }

  Admin.views.reimbursements = {
    path: '/reimbursements',
    title: '月度报销单',
    breadcrumb: ['财务与报销', '月度报销单'],

    async render(el) {
      var ui = Admin.ui;
      el.appendChild(ui.h('div', { class: 'page-head' },
        ui.h('h2', { text: '月度报销单' }),
        ui.h('span', { class: 'sub', text: '按月汇总已签字工单的耗材金额 → 科室签字 → 财务回填' })
      ));
      el.appendChild(ui.h('div', { id: 'reimb-body' }, ui.skeletonLines(6)));
      await paint(el);
    }
  };
})(window);
