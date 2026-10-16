/*!
 * 视图：财务与报销 · 月度报销单（#/reimbursements）—— 按科室分池模式
 * 功能：科室池卡片网格（只显示有数据的科室）+ 进入科室抽屉（勾选待报销工单生成 + 已有报销单列表/财务回填）
 * 设计依据：2026-10-16《报销单设计》第 1 步（分池 + 勾选生成）
 * 不做：撤回/退回、快照隔离表、拼音搜索、草稿合并、导出打印（过度设计）
 */
(function (global) {
  'use strict';
  var Admin = global.Admin = global.Admin || {};
  Admin.views = Admin.views || {};

  var state = { pool: [], rows: [] };

  function canGenerate() { return Admin.store.isAdminLike(); }
  function canReimburse() { return Admin.store.roleCode() === 'FINANCE'; }

  async function load() {
    state.pool = await Admin.api.get('/api/reimbursements/pool') || [];
    // 财务不需要池，但仍看全量报销单列表用于回填
    if (!canGenerate()) {
      state.rows = await Admin.api.get('/api/reimbursements') || [];
    }
  }

  /* ---------------- 科室池卡片 ---------------- */
  function poolCard(dept) {
    var ui = Admin.ui;
    var badges = [];
    if (dept.pendingWorkOrders) {
      badges.push(ui.h('span', { class: 'pool-badge tone-danger', text: '待报销工单 ' + dept.pendingWorkOrders }));
    }
    if (dept.pendingSignCount) {
      badges.push(ui.h('span', { class: 'pool-badge tone-warn', text: '待签字 ' + dept.pendingSignCount }));
    }
    if (dept.pendingReimburseCount) {
      badges.push(ui.h('span', { class: 'pool-badge tone-primary', text: '待报销单 ' + dept.pendingReimburseCount }));
    }
    if (dept.reimbursedCount) {
      badges.push(ui.h('span', { class: 'pool-badge tone-success', text: '已报销 ' + dept.reimbursedCount }));
    }
    if (dept.voidedCount) {
      badges.push(ui.h('span', { class: 'pool-badge tone-muted', text: '已作废 ' + dept.voidedCount }));
    }
    if (!badges.length) {
      badges.push(ui.h('span', { class: 'pool-badge tone-muted', text: '暂无待处理' }));
    }
    return ui.h('div', { class: 'pool-card' },
      ui.h('div', { class: 'pool-card-head' },
        ui.h('span', { class: 'pool-dept-name', text: dept.departmentName }),
        ui.h('span', { class: 'pool-time', text: dept.latestAt ? ui.fmtTime(dept.latestAt) : '' })
      ),
      ui.h('div', { class: 'pool-badges', },
        badges.reduce(function (arr, b) { arr.push(b); return arr; }, [])
      ),
      ui.h('button', {
        class: 'btn btn-primary btn-sm pool-enter', text: '进入',
        onclick: function () { openDeptDrawer(dept.departmentId, dept.departmentName); }
      })
    );
  }

  /* ---------------- 科室抽屉（勾选生成 + 已有报销单） ---------------- */
  async function openDeptDrawer(deptId, deptName) {
    var ui = Admin.ui;
    var drawer = ui.drawer({ title: deptName + ' · 待报销池', body: ui.skeletonLines(6) });

    async function fill() {
      var pending = [];
      var reimbs = [];
      try {
        pending = await Admin.api.get('/api/work-orders/by-department', { departmentId: deptId, status: 'PENDING_REIMBURSE' }) || [];
      } catch (e) { /* 无权限或空 */ }
      try {
        reimbs = await Admin.api.get('/api/reimbursements') || [];
        reimbs = reimbs.filter(function (r) { return r.departmentId === deptId; });
      } catch (e) { /* ignore */ }

      var selected = {};

      // 已有报销单列表
      var reimbList = reimbs.length
        ? ui.h('div', { class: 'detail-section' },
          ui.h('h4', { text: '该科室报销单（' + reimbs.length + '）' }),
          ui.h('div', { class: 'table-wrap' },
            ui.h('table', { class: 'data' },
              ui.h('thead', null, ui.h('tr', null,
                ui.h('th', { text: 'ID' }), ui.h('th', { text: '月份' }),
                ui.h('th', { text: '金额' }), ui.h('th', { text: '状态' }),
                ui.h('th', { text: '操作' }))),
              ui.h('tbody', null, reimbs.map(function (r) {
                return ui.h('tr', null,
                  ui.h('td', { html: '<span class="cell-strong">#' + r.id + '</span>' }),
                  ui.h('td', { text: r.month }),
                  ui.h('td', { text: ui.money(r.totalAmount) }),
                  ui.h('td', null, ui.statusBadge(ui.STATUS.REIMBURSEMENT, r.status)),
                  ui.h('td', null, reimbActions(r, drawer))
                );
              }))
            )
          )
        )
        : ui.h('div', { style: { color: '#6E7770', fontSize: '13px', margin: '8px 0' }, text: '该科室暂无报销单' });

      // 待报销工单列表 + 勾选生成
      var woBlock;
      if (canGenerate()) {
        var checkboxes = [];
        var woTable = pending.length
          ? ui.h('div', { class: 'table-wrap' },
            ui.h('table', { class: 'data' },
              ui.h('thead', null, ui.h('tr', null,
                ui.h('th', { text: '勾选' }), ui.h('th', { text: '工单' }),
                ui.h('th', { text: '设备/标题' }), ui.h('th', { text: '签字人' }),
                ui.h('th', { text: '签字时间' }))),
              ui.h('tbody', null, pending.map(function (wo) {
                var cb = ui.h('input', { type: 'checkbox', dataset: { id: wo.id }, onchange: function (e) {
                  if (e.target.checked) selected[wo.id] = true; else delete selected[wo.id];
                } });
                checkboxes.push(cb);
                return ui.h('tr', null,
                  ui.h('td', null, cb),
                  ui.h('td', { html: '<span class="cell-strong">#' + wo.id + '</span>' }),
                  ui.h('td', { text: wo.title || '-' }),
                  ui.h('td', { text: wo.signedBy || '-' }),
                  ui.h('td', { text: wo.signedAt ? ui.fmtTime(wo.signedAt) : '-' })
                );
              }))
            )
          )
          : ui.h('div', { style: { color: '#6E7770', fontSize: '13px', margin: '8px 0' }, text: '该科室暂无待报销工单（需护士长签字验收后才会进入此池）' });

        var genBtn = pending.length ? ui.h('button', {
          class: 'btn btn-primary btn-sm', style: { marginTop: '10px' }, text: '勾选生成报销单',
          onclick: async function () {
            var ids = Object.keys(selected).map(function (k) { return Number(k); });
            if (!ids.length) { ui.toast('请先勾选要生成报销单的工单', 'warn'); return; }
            try {
              await Admin.api.post('/api/reimbursements/generate-from-pool', { departmentId: deptId, workOrderIds: ids });
              ui.toast('已生成报销单，待护士长签字确认', 'success');
              drawer.close();
              await refresh(document.querySelector('.content') || document.body);
            } catch (e) {
              ui.toast(e.message, 'error');
            }
          }
        }) : null;

        woBlock = ui.h('div', { class: 'detail-section' },
          ui.h('h4', { text: '待报销工单（' + pending.length + '）' }),
          woTable,
          genBtn
        );
      } else {
        woBlock = ui.h('div', { class: 'detail-section' },
          ui.h('h4', { text: '待报销工单（' + pending.length + '）' }),
          ui.h('div', { style: { color: '#6E7770', fontSize: '13px' }, text: pending.length + ' 条待报销工单（生成由管理员操作）' })
        );
      }

      drawer.setBody(ui.h('div', null, woBlock, reimbList));
    }

    await fill();
  }

  function reimbActions(r, drawer) {
    var ui = Admin.ui;
    var nodes = [ui.h('button', {
      class: 'btn btn-ghost btn-sm', text: '详情',
      onclick: function () { openDetail(r); }
    })];
    if (r.status === 'PENDING_REIMBURSE' && canReimburse()) {
      nodes.push(ui.h('button', {
        class: 'btn btn-success btn-sm', style: { marginLeft: '6px' }, text: '财务回填',
        onclick: async function () {
          var ok = await ui.confirm({
            title: '财务回填确认',
            message: '确认将 ' + r.month + ' · ' + (r.departmentName || '') + ' 的报销单（' + ui.money(r.totalAmount) + '）标记为「已报销」？',
            confirmText: '确认已报销'
          });
          if (!ok) return;
          try {
            await Admin.api.post('/api/reimbursements/' + r.id + '/reimburse');
            ui.toast('已标记为已报销，关联工单与报修单状态已同步更新', 'success');
            drawer.close();
            await refresh(document.querySelector('.content') || document.body);
            Admin.refreshBadges();
          } catch (e) {
            ui.toast(e.message, 'error');
          }
        }
      }));
    }
    return ui.h('div', null, nodes);
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
            : ui.h('div', { style: { color: '#6E7770', fontSize: '13px' }, text: '无签名图片（历史单据签字时未启用留痕）' })
        ),
        ui.h('div', { class: 'detail-section' },
          ui.h('h4', { text: '包含工单（' + wos.length + '）' }),
          woTable
        )
      ));
    } catch (e) {
      drawer.setBody(ui.empty('加载失败：' + e.message));
    }
  }

  function detailRow(k, v) {
    var ui = Admin.ui;
    return ui.h('div', { class: 'detail-row' },
      ui.h('span', { class: 'k', text: k }),
      ui.h('span', { class: 'v' }, v instanceof global.Node ? v : String(v === null || v === undefined || v === '' ? '未填写' : v))
    );
  }

  function summaryCard() {
    var ui = Admin.ui;
    var poolTotal = 0, pendingSign = 0, pendingReimburse = 0, reimbursed = 0;
    state.pool.forEach(function (d) {
      poolTotal += Number(d.pendingWorkOrders || 0);
      pendingSign += Number(d.pendingSignCount || 0);
      pendingReimburse += Number(d.pendingReimburseCount || 0);
      reimbursed += Number(d.reimbursedCount || 0);
    });
    return ui.h('div', { class: 'grid grid-metrics' },
      ui.metricCard({ title: '待报销工单', value: poolTotal, unit: '条', icon: Admin.layout.ICONS.clipboard, tone: 'danger', hint: '已签字待归集' }),
      ui.metricCard({ title: '待护士长确认', value: pendingSign, unit: '张', icon: Admin.layout.ICONS.history, tone: 'warn', hint: '管理员已生成，等签字' }),
      ui.metricCard({ title: '待财务回填', value: pendingReimburse, unit: '张', icon: Admin.layout.ICONS.clipboard, tone: 'primary', hint: '已签字，待财务' }),
      ui.metricCard({ title: '已报销', value: reimbursed, unit: '张', icon: Admin.layout.ICONS.info, tone: 'success', hint: '流程闭环完成' })
    );
  }

  async function paint(el) {
    var ui = Admin.ui;
    await load();

    // 财务视角：直接看全量报销单列表回填（不需要池）
    if (!canGenerate()) {
      var list = ui.listSection({
        title: '月度报销单',
        hint: '（待财务审核的报销单）',
        searchPlaceholder: '搜索月份 / 科室 / 签字人',
        searchFields: ['month', 'departmentName', 'signedBy'],
        rows: state.rows.filter(function (r) { return r.status === 'PENDING_REIMBURSE'; }),
        empty: '暂无待回填报销单',
        pageSize: 10,
        extra: ui.h('button', { class: 'btn btn-ghost btn-sm', text: '刷新', onclick: function () { refresh(el); } }),
        columns: [
          { key: 'id', title: 'ID', render: function (r) { return '<span class="cell-strong">' + r.id + '</span>'; } },
          { key: 'month', title: '月份' },
          { key: 'departmentName', title: '科室', wrap: true },
          { key: 'totalAmount', title: '合计金额', align: 'right', render: function (r) { return '<b>' + ui.money(r.totalAmount) + '</b>'; } },
          { key: 'status', title: '状态', render: function (r) { return ui.statusBadge(ui.STATUS.REIMBURSEMENT, r.status); } },
          { key: 'signedBy', title: '签字人', render: function (r) { return ui.esc(r.signedBy || '-'); } },
          {
            title: '操作', render: function (r) {
              return ui.h('div', null,
                ui.h('button', { class: 'btn btn-ghost btn-sm', text: '详情', onclick: function () { openDetail(r); } }),
                ui.h('button', {
                  class: 'btn btn-success btn-sm', style: { marginLeft: '6px' }, text: '财务回填',
                  onclick: async function () {
                    var ok = await ui.confirm({ title: '财务回填确认', message: '确认将此报销单（' + ui.money(r.totalAmount) + '）标记为已报销？', confirmText: '确认已报销' });
                    if (!ok) return;
                    try {
                      await Admin.api.post('/api/reimbursements/' + r.id + '/reimburse');
                      ui.toast('已标记为已报销，关联工单与报修单状态已同步更新', 'success');
                      await refresh(el);
                      Admin.refreshBadges();
                    } catch (e) { ui.toast(e.message, 'error'); }
                  }
                })
              );
            }
          }
        ]
      });
      var host = el.querySelector('#reimb-body');
      host.innerHTML = '';
      host.appendChild(list.node);
      Admin.refreshBadges();
      return;
    }

    // 管理员视角：科室池卡片网格
    var grid = ui.h('div', { class: 'pool-grid' },
      state.pool.length
        ? state.pool.map(poolCard)
        : [ui.h('div', { class: 'empty', text: '暂无有待报销工单或报销单的科室' })]
    );

    var host = el.querySelector('#reimb-body');
    host.innerHTML = '';
    host.appendChild(summaryCard());
    host.appendChild(ui.h('div', { class: 'pool-head' },
      ui.h('h3', { text: '科室待报销池' }),
      ui.h('span', { class: 'sub', text: '（只显示有数据的科室；进入科室勾选工单生成报销单）' })
    ));
    host.appendChild(grid);
    Admin.refreshBadges();
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
        ui.h('span', {
          class: 'sub',
          text: canGenerate()
            ? '按科室分池管理待报销工单 → 勾选工单生成报销单 → 护士长签字 → 财务回填'
            : '财务岗位：核对已签字报销单后执行「财务回填」，工单与报修单状态将同步为已报销'
        })
      ));
      el.appendChild(ui.h('div', { id: 'reimb-body' }, ui.skeletonLines(6)));
      await paint(el);
    }
  };
})(window);
