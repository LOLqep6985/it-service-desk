/*!
 * 视图：工作台 · 首页概览（#/dashboard）
 * 只展示高价值聚合数据：快捷操作 + 指标卡 + 待办聚焦 + 本月报销 + 近 7 日趋势
 */
(function (global) {
  'use strict';
  var Admin = global.Admin = global.Admin || {};
  Admin.views = Admin.views || {};

  function pad(n) { return n < 10 ? '0' + n : String(n); }
  function todayKey() {
    var d = new Date();
    return d.getFullYear() + '-' + pad(d.getMonth() + 1) + '-' + pad(d.getDate());
  }
  function greeting() {
    var hh = new Date().getHours();
    if (hh < 6) return '凌晨好';
    if (hh < 12) return '早上好';
    if (hh < 18) return '下午好';
    return '晚上好';
  }
  function dateText() {
    var d = new Date();
    var week = '日一二三四五六'.charAt(d.getDay());
    return d.getFullYear() + '-' + pad(d.getMonth() + 1) + '-' + pad(d.getDate()) + ' 星期' + week;
  }

  function heroBtn(text, path) {
    return Admin.ui.h('button', {
      class: 'hero-btn', text: text,
      onclick: function () { Admin.router.navigate(path); }
    });
  }

  function heroNode(stats) {
    var user = Admin.store.currentUser() || {};
    return Admin.ui.h('div', { class: 'hero' },
      Admin.ui.h('div', null,
        Admin.ui.h('h2', { text: greeting() + '，' + (user.name || '管理员') }),
        Admin.ui.h('div', {
          class: 'sub',
          text: dateText() + ' · 待受理 ' + stats.pendingReports + ' 单 · 待科室验收 ' + stats.pendingAcceptWorkOrders + ' 单'
        })
      ),
      Admin.ui.h('div', { class: 'hero-actions' },
        heroBtn('去派单', '/reports/pending'),
        heroBtn('工单管理', '/work-orders'),
        heroBtn('月度报销单', '/reimbursements'),
        heroBtn('科室索引', '/departments')
      )
    );
  }

  function metricsNode(stats) {
    var ui = Admin.ui;
    var diff = stats.todayReports - stats.yesterdayReports;
    var delta = diff > 0
      ? { text: '↑ ' + diff + ' 较昨日', tone: 'up' }
      : (diff < 0 ? { text: '↓ ' + Math.abs(diff) + ' 较昨日', tone: 'down' } : { text: '与昨日持平', tone: '' });

    var grid = ui.h('div', { class: 'grid grid-metrics' },
      ui.metricCard({
        title: '今日新增报修单', value: stats.todayReports, unit: '单',
        icon: Admin.layout.ICONS.clipboard, delta: delta, hint: '昨日 ' + stats.yesterdayReports + ' 单'
      }),
      ui.metricCard({
        title: '待受理 / 待派单', value: stats.pendingReports, unit: '单',
        icon: Admin.layout.ICONS.clipboard, tone: 'danger', hint: '需尽快派单'
      }),
      ui.metricCard({
        title: '进行中工单', value: stats.activeWorkOrders, unit: '单',
        icon: Admin.layout.ICONS.wrench, tone: 'warn',
        hint: '已派单 ' + stats.assignedWorkOrders + ' · 处理中 ' + stats.inProgressWorkOrders
      }),
      ui.metricCard({
        title: '待科室验收', value: stats.pendingAcceptWorkOrders, unit: '单',
        icon: Admin.layout.ICONS.history, tone: 'success', hint: '等待护士长签字确认'
      })
    );
    return grid;
  }

  function pendingReportsCard(list) {
    var ui = Admin.ui;
    var body = ui.dataTable({
      columns: [
        { key: 'reportNo', title: '报修单号', render: function (r) { return '<span class="cell-strong">' + ui.esc(r.reportNo || r.id) + '</span>'; } },
        { key: 'departmentName', title: '科室', wrap: true },
        { key: 'deviceName', title: '设备' },
        { key: 'reporterName', title: '报修人' },
        { key: 'createdAt', title: '提交时间', render: function (r) { return ui.fmtTime(r.createdAt); } }
      ],
      rows: list.slice(0, 5),
      empty: '暂无待受理报修单'
    });
    return ui.card({
      title: '待办 · 待受理报修单',
      hint: '（' + list.length + '）',
      extra: ui.h('button', {
        class: 'btn btn-primary btn-sm', text: '去派单',
        onclick: function () { Admin.router.navigate('/reports/pending'); }
      }),
      body: body
    });
  }

  function pendingAcceptCard(list, userMap) {
    var ui = Admin.ui;
    var body = ui.dataTable({
      columns: [
        { key: 'id', title: '工单', render: function (r) { return '<span class="cell-strong">#' + r.id + '</span>'; } },
        { key: 'title', title: '标题', wrap: true },
        { key: 'departmentName', title: '科室', wrap: true },
        { key: 'assigneeId', title: '维修工', render: function (r) {
          var u = userMap[r.assigneeId];
          return ui.esc(u ? u.name : (r.assigneeId || '-'));
        } },
        { key: 'createdAt', title: '派单时间', render: function (r) { return ui.fmtTime(r.createdAt); } }
      ],
      rows: list.slice(0, 5),
      empty: '暂无待验收工单'
    });
    return ui.card({
      title: '待办 · 待科室验收',
      hint: '（' + list.length + '）',
      extra: ui.h('button', {
        class: 'btn btn-ghost btn-sm', text: '查看工单',
        onclick: function () { Admin.router.navigate('/work-orders'); }
      }),
      body: body
    });
  }

  function reimbursementCard(stats) {
    var ui = Admin.ui;
    var body = ui.h('div', null,
      ui.h('div', { style: { display: 'flex', alignItems: 'baseline', gap: '8px', marginBottom: '14px' } },
        ui.h('span', { style: { fontSize: '26px', fontWeight: '600', color: '#1f2937' }, text: ui.money(stats.monthReimbursementAmount) }),
        ui.h('span', { style: { fontSize: '12px', color: '#7f8c8d' }, text: stats.month + ' · 共 ' + stats.monthReimbursementTotal + ' 单' })
      ),
      ui.distBars([
        { label: '待签字', value: stats.monthReimbursementPendingSign, color: '#f39c12' },
        { label: '待报销', value: stats.monthReimbursementPendingReimburse, color: '#1d6fe0' },
        { label: '已报销', value: stats.monthReimbursementReimbursed, color: '#27ae60' }
      ])
    );
    return ui.card({
      title: '本月报销概览',
      extra: ui.h('button', {
        class: 'btn btn-ghost btn-sm', text: '去处理',
        onclick: function () { Admin.router.navigate('/reimbursements'); }
      }),
      body: body
    });
  }

  function trendCard(stats) {
    var ui = Admin.ui;
    return ui.card({
      title: '近 7 日报修量',
      hint: '（含今日）',
      body: ui.trendBars(stats.reportTrend || [], { todayKey: todayKey() })
    });
  }

  Admin.views.dashboard = {
    path: '/dashboard',
    title: '首页概览',
    breadcrumb: ['工作台', '首页概览'],

    async render(el) {
      var ui = Admin.ui;
      var heroHost = ui.h('div');
      var metricHost = ui.h('div');
      var todoHost = ui.h('div', { class: 'grid grid-2' });
      var bottomHost = ui.h('div', { class: 'grid grid-2' });

      metricHost.appendChild(ui.skeletonCards(4));
      todoHost.appendChild(ui.h('div', { class: 'skeleton skeleton-card', style: { height: '220px' } }));
      todoHost.appendChild(ui.h('div', { class: 'skeleton skeleton-card', style: { height: '220px' } }));
      bottomHost.appendChild(ui.h('div', { class: 'skeleton skeleton-card', style: { height: '200px' } }));
      bottomHost.appendChild(ui.h('div', { class: 'skeleton skeleton-card', style: { height: '200px' } }));

      el.appendChild(ui.h('div', { class: 'page-head' },
        ui.h('h2', { text: '首页概览' }),
        ui.h('span', { class: 'sub', text: '聚合关键指标与待办，具体操作请从左侧菜单进入' })
      ));
      el.appendChild(heroHost);
      el.appendChild(metricHost);
      el.appendChild(todoHost);
      el.appendChild(bottomHost);

      var stats = await Admin.api.get('/api/stats/dashboard');
      var [pendingReports, pendingAccept] = await Promise.all([
        Admin.api.get('/api/reports', { status: 'PENDING' }),
        Admin.api.get('/api/work-orders', { status: 'PENDING_ACCEPT' })
      ]);
      var userMap = {};
      try {
        await Admin.store.loadUsers();
        userMap = Admin.store.userMap();
      } catch (e) { /* 用户信息拿不到不影响主指标 */ }

      heroHost.innerHTML = '';
      heroHost.appendChild(heroNode(stats));
      metricHost.innerHTML = '';
      metricHost.appendChild(metricsNode(stats));
      todoHost.innerHTML = '';
      todoHost.appendChild(pendingReportsCard(pendingReports || []));
      todoHost.appendChild(pendingAcceptCard(pendingAccept || [], userMap));
      bottomHost.innerHTML = '';
      bottomHost.appendChild(reimbursementCard(stats));
      bottomHost.appendChild(trendCard(stats));
    }
  };
})(window);
