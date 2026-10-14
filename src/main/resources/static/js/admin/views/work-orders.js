/*!
 * 视图：工单业务 · 工单管理（#/work-orders）
 * 功能：工单列表（状态过滤/搜索/分页）+ 详情抽屉（报修信息/照片/流转/维修记录/耗材核价/签字留痕）
 */
(function (global) {
  'use strict';
  var Admin = global.Admin = global.Admin || {};
  Admin.views = Admin.views || {};

  var state = { rows: [], status: '', keyword: '', userMap: {} };

  var STATUS_OPTIONS = [
    { value: '', label: '全部状态' },
    { value: 'ASSIGNED', label: '已派单' },
    { value: 'IN_PROGRESS', label: '处理中' },
    { value: 'PENDING_ACCEPT', label: '待验收' },
    { value: 'COMPLETED', label: '已完成' }
  ];

  async function load() {
    state.rows = await Admin.api.get('/api/work-orders', state.status ? { status: state.status } : null) || [];
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
    if (!url) return ui.h('div', { style: { color: '#95a5a6', fontSize: '13px' }, text: '无' });
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
                var input = ui.h('input', {
                  class: 'input input-sm', type: 'number', step: '0.01', style: { width: '110px' },
                  value: c.unitPrice === null || c.unitPrice === undefined ? '' : c.unitPrice,
                  placeholder: '未核价', dataset: { id: c.id }
                });
                return ui.h('tr', null,
                  ui.h('td', { text: c.name || '-' }),
                  ui.h('td', { text: c.quantity === null || c.quantity === undefined ? 1 : c.quantity }),
                  ui.h('td', null, input)
                );
              }))
            )
          ),
          ui.h('button', {
            class: 'btn btn-primary btn-sm', style: { marginTop: '10px' }, text: '保存核价',
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
        : ui.h('div', { style: { color: '#95a5a6', fontSize: '13px' }, text: '暂无耗材记录' });

      drawer.setBody(ui.h('div', null,
        section('基本信息', [
          ['工单ID', wo.id],
          ['报修单号', reportNoOf(wo)],
          ['科室', wo.departmentName || (report && report.departmentName)],
          ['设备名称', (report && report.deviceName) || wo.title],
          ['报修人', report && report.reporterName],
          ['维修工', repairerName(wo.assigneeId)]
        ]),
        section('报修信息', [
          ['故障描述', (report && report.problemDesc) || wo.description],
          ['设备照片', photoBlock(report && report.devicePhoto, '设备照片')]
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
          ['签字人', wo.signedBy],
          ['签字时间', wo.signedAt ? ui.fmtTime(wo.signedAt) : '未签字'],
          ['手写签名', photoBlock(wo.signatureUrl, '手写签名')]
        ])
      ));
    }

    await fill();
    return drawer;
  }

  /* ---------------- 页面 ---------------- */
  async function paint(el) {
    var ui = Admin.ui;
    await load();

    var statusSelect = ui.h('select', {
      class: 'input input-sm', style: { width: '130px' },
      onchange: function (e) { state.status = e.target.value; refresh(el); }
    }, STATUS_OPTIONS.map(function (o) {
      return ui.h('option', { value: o.value, text: o.label, selected: o.value === state.status });
    }));

    var list = ui.listSection({
      title: '工单管理',
      hint: '（派单后生成）',
      searchPlaceholder: '搜索工单号 / 标题 / 科室 / 维修工',
      searchFields: ['id', 'title', 'departmentName'],
      rows: state.rows,
      empty: '暂无工单',
      pageSize: 10,
      controls: statusSelect,
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
        { key: 'signStatus', title: '签字', render: function (r) { return ui.esc(r.signStatus === 'SIGNED' ? '已签字' : '待签字'); } },
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
