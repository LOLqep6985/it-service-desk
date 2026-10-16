/*!
 * 视图：工单业务 · 工单管理（#/work-orders）
 * 功能：工单列表（状态过滤/搜索/分页）+ 详情抽屉（报修信息/流转/维修记录/耗材核价/签字留痕）
 * 说明：后台只管工单 —— 报修单是科室端概念，不在此重复展示（历史决策见 2026-10-16 两轮收敛）
 */
(function (global) {
  'use strict';
  var Admin = global.Admin = global.Admin || {};
  Admin.views = Admin.views || {};

  var state = { rows: [], scope: 'ALL', keyword: '', userMap: {} };

  var SCOPE_OPTIONS = [
    { value: 'ALL', label: '全部状态（默认）' },
    { value: 'ASSIGNED', label: '待接单' },
    { value: 'IN_PROGRESS', label: '维修中' },
    { value: 'PENDING_PRICING', label: '待核价' },
    { value: 'PENDING_SIGN', label: '待签字' },
    { value: 'PENDING_REIMBURSE', label: '待报销' },
    { value: 'REIMBURSED', label: '已报销' }
  ];

  /** 全量加载后在浏览器端按状态过滤 */
  function visibleRows() {
    if (state.scope === 'ALL') return state.rows;
    return state.rows.filter(function (w) { return w.status === state.scope; });
  }

  async function load() {
    // 一次取全量后在浏览器端按范围过滤（服务端分页为已知待优化项）
    state.rows = await Admin.api.get('/api/work-orders') || [];
    try {
      await Admin.store.loadUsers(true);
      state.userMap = Admin.store.userMap();
    } catch (e) { /* 姓名映射失败不阻塞列表 */ }
  }

  function repairerName(id) {
    var u = state.userMap[id];
    return u ? u.name : (id || '-');
  }

  function reportNoOf(wo) {
    return wo.reportId ? ('R' + String(wo.reportId).padStart(6, '0')) : '-';
  }

  /* ---------------- 详情抽屉 ---------------- */
  function section(title, rows) {
    var ui = Admin.ui;
    return ui.h('div', { class: 'detail-section' },
      ui.h('h4', { text: title }),
      rows.map(function (r) {
        return ui.h('div', { class: 'detail-row' },
          ui.h('span', { class: 'k', text: r[0] }),
          ui.h('span', { class: 'v' }, r[1] instanceof global.Node ? r[1] : String(r[1] === null || r[1] === undefined || r[1] === '' ? '未填写' : r[1]))
        );
      })
    );
  }

  function photoBlock(url, alt) {
    var ui = Admin.ui;
    if (!url) return ui.h('div', { style: { color: '#6E7770', fontSize: '13px' }, text: '无' });
    return ui.h('div', { class: 'detail-photo' },
      ui.h('img', { src: url, alt: alt || '照片', onclick: function (e) { global.open(e.target.src, '_blank'); } })
    );
  }

  async function openDetail(wo, onPriced) {
    var ui = Admin.ui;

    var drawer = ui.drawer({ title: '工单 #' + wo.id + ' 详情', body: ui.skeletonLines(6) });

    async function fill() {
      var report = null;
      var consumables = [];
      var actions = [];
      var reportNo = reportNoOf(wo);
      var reimbursement = null;
      // 服务端计算的动作集合（动态权限）：前端按钮据此渲染
      try {
        var detail = await Admin.api.get('/api/work-orders/' + wo.id);
        actions = (detail && detail.allowedActions) || [];
        if (detail && detail.reportNo) reportNo = detail.reportNo;
        if (detail) reimbursement = detail.reimbursement;
      } catch (e) { /* 拉取失败时退化为只读展示 */ }
      try {
        if (wo.reportId) {
          report = await Admin.api.get('/api/reports/' + wo.reportId);
        }
      } catch (e) { /* 报修单可能已被清理，忽略 */ }
      try {
        consumables = await Admin.api.get('/api/work-orders/' + wo.id + '/consumables') || [];
      } catch (e) { /* 无权限或为空 */ }

      var consTable = consumables.length
        ? ui.h('div', null,
          ui.h('div', { class: 'table-wrap' },
            ui.h('table', { class: 'data' },
              ui.h('thead', null, ui.h('tr', null,
                ui.h('th', { text: '耗材' }), ui.h('th', { text: '数量' }), ui.h('th', { text: '实际单价（核价）' }))),
              ui.h('tbody', null, consumables.map(function (c) {
                // 核价是管理员/调度员的动作（PRICE）：无权限时只读展示
                var priceCell = ui.can(actions, 'PRICE')
                  ? ui.h('input', {
                    class: 'input input-sm', type: 'number', step: '0.01', style: { width: '110px' },
                    value: c.unitPrice === null || c.unitPrice === undefined ? '' : c.unitPrice,
                    placeholder: '未核价', dataset: { id: c.id }
                  })
                  : ui.h('span', { text: c.unitPrice === null || c.unitPrice === undefined ? '未核价' : ui.money(c.unitPrice) });
                return ui.h('tr', null,
                  ui.h('td', { text: c.name || '-' }),
                  ui.h('td', { text: c.quantity === null || c.quantity === undefined ? 1 : c.quantity }),
                  ui.h('td', null, priceCell)
                );
              }))
            )
          ),
          ui.h('button', {
            class: 'btn btn-primary btn-sm', style: { marginTop: '10px' }, text: '保存核价',
            hidden: !ui.can(actions, 'PRICE'),
            onclick: async function () {
              var items = [];
              Array.prototype.forEach.call(drawer.bodyEl.querySelectorAll('input[data-id]'), function (inp) {
                var v = inp.value.trim();
                items.push({ id: Number(inp.getAttribute('data-id')), unitPrice: v === '' ? null : Number(v) });
              });
              try {
                await Admin.api.post('/api/work-orders/' + wo.id + '/consumables/pricing', { consumables: items });
                ui.toast('核价已保存', 'success');
                if (onPriced) onPriced();
              } catch (e) {
                ui.toast(e.message, 'error');
              }
            }
          })
        )
        : ui.h('div', { style: { color: '#6E7770', fontSize: '13px' }, text: '暂无耗材记录' });

      drawer.setBody(ui.h('div', null,
        section('基本信息', [
          ['工单ID', wo.id],
          ['报修单号', reportNo],
          ['科室', wo.departmentName || (report && report.departmentName)],
          ['设备名称', (report && report.deviceName) || wo.title],
          ['报修人', report && report.reporterName],
          ['维修工', repairerName(wo.assigneeId)]
        ]),
        section('报修信息', [
          ['故障描述', (report && report.problemDesc) || wo.description]
        ]),
        section('流转记录', [
          ['派单时间', ui.fmtTime(wo.createdAt)],
          ['分配的维修工', repairerName(wo.assigneeId)],
          ['完成时间', wo.completedAt ? ui.fmtTime(wo.completedAt) : '未完成']
        ]),
        section('维修记录', [['处理说明', wo.repairRecord]]),
        ui.h('div', { class: 'detail-section' }, ui.h('h4', { text: '耗材更换记录' }), consTable),
        section('签字状态', [
          ['签字状态', wo.signStatus === 'SIGNED'
            ? ui.badge('已签字', 'success')
            : ui.badge('待签字', 'pending')],
          ['签字方式', ui.signTypeBadge(wo.signType)],
          ['签字人', wo.signedBy],
          ['签字时间', wo.signedAt ? ui.fmtTime(wo.signedAt) : '未签字'],
          ['签名图片', photoBlock(wo.signatureUrl, '签名图')],
          // 财务剥离：待报销不是工单状态，工单只展示它被纳入的报销单进度
          ['报销进度', reimbursement
            ? ui.h('span', null,
              ui.h('span', { text: reimbursement.month + ' 报销单 #' + reimbursement.id + ' · ' }),
              ui.statusBadge(ui.STATUS.REIMBURSEMENT, reimbursement.status))
            : '尚未纳入报销']
        ])
        // 注：此处原有「确认月度报销」按钮已按职责分离移除 ——
        // 报销是财务在「月度报销单」页面对整张报销单执行的回填动作，运维侧不代填。
      ));
    }

    await fill();
    return drawer;
  }

  /* ---------------- 页面 ---------------- */
  async function paint(el) {
    var ui = Admin.ui;
    await load();

    var scopeSelect = ui.h('select', {
      class: 'input input-sm', style: { width: '160px' },
      onchange: function (e) { state.scope = e.target.value; refresh(el); }
    }, SCOPE_OPTIONS.map(function (o) {
      return ui.h('option', { value: o.value, text: o.label, selected: o.value === state.scope });
    }));

    var list = ui.listSection({
      title: '工单管理',
      hint: '（派单后生成；默认显示全部状态）',
      searchPlaceholder: '搜索工单号 / 标题 / 科室 / 维修工',
      searchFields: ['id', 'title', 'departmentName'],
      rows: visibleRows(),
      empty: '暂无工单',
      pageSize: 10,
      controls: scopeSelect,
      extra: ui.h('button', {
        class: 'btn btn-ghost btn-sm', text: '刷新',
        onclick: function () { refresh(el); }
      }),
      columns: [
        { key: 'id', title: '工单ID', render: function (r) { return '<span class="cell-strong">#' + r.id + '</span>'; } },
        { key: 'reportId', title: '报修单号', render: reportNoOf },
        { key: 'title', title: '设备/标题', wrap: true },
        { key: 'departmentName', title: '科室', wrap: true },
        { key: 'assigneeId', title: '维修工', render: function (r) { return ui.esc(repairerName(r.assigneeId)); } },
        { key: 'status', title: '状态', render: function (r) { return ui.statusBadge(ui.STATUS.WORK_ORDER, r.status); } },
        { key: 'createdAt', title: '创建时间', render: function (r) { return ui.fmtTime(r.createdAt); } },
        { key: 'completedAt', title: '完成时间', render: function (r) { return r.completedAt ? ui.fmtTime(r.completedAt) : '-'; } },
        {
          title: '操作', render: function (r) {
            return ui.h('button', {
              class: 'btn btn-primary btn-sm', text: '详情',
              onclick: function () { openDetail(r, function () { refresh(el); }); }
            });
          }
        }
      ]
    });

    var host = el.querySelector('#wo-body');
    host.innerHTML = '';
    host.appendChild(list.node);
    Admin.refreshBadges();
  }

  async function refresh(el) {
    var host = el.querySelector('#wo-body');
    host.innerHTML = '';
    host.appendChild(Admin.ui.skeletonLines(6));
    await paint(el);
  }

  Admin.views.workOrders = {
    path: '/work-orders',
    title: '工单管理',
    breadcrumb: ['工单业务管理', '工单管理'],

    async render(el) {
      var ui = Admin.ui;
      el.appendChild(ui.h('div', { class: 'page-head' },
        ui.h('h2', { text: '工单管理' }),
        ui.h('span', { class: 'sub', text: '按状态跟进工单，打开详情可核价耗材并查看签字留痕' })
      ));
      el.appendChild(ui.h('div', { id: 'wo-body' }, ui.skeletonLines(6)));
      await paint(el);
    }
  };
})(window);
