/*!
 * 视图：系统与基础数据配置 · 用户管理（#/users）
 * 功能：用户列表（按角色过滤 + 搜索 + 分页）；接口已脱敏，不返回密码
 */
(function (global) {
  'use strict';
  var Admin = global.Admin = global.Admin || {};
  Admin.views = Admin.views || {};

  var state = { rows: [], role: '' };

  var ROLE_OPTIONS = [
    { value: '', label: '全部角色' },
    { value: 'ADMIN', label: '管理员' },
    { value: 'DISPATCHER', label: '调度员' },
    { value: 'REPAIRER', label: '维修工' }
  ];

  var ROLE_TONE = { ADMIN: 'danger', DISPATCHER: 'warn', REPAIRER: 'success', REPORTER: 'muted' };

  async function load() {
    state.rows = await Admin.api.get('/api/users', state.role ? { role: state.role } : null) || [];
  }

  async function paint(el) {
    var ui = Admin.ui;
    await load();

    var roleSelect = ui.h('select', {
      class: 'input input-sm', style: { width: '130px' },
      onchange: function (e) { state.role = e.target.value; refresh(el); }
    }, ROLE_OPTIONS.map(function (o) {
      return ui.h('option', { value: o.value, text: o.label, selected: o.value === state.role });
    }));

    var list = ui.listSection({
      title: '用户管理',
      hint: '（接口已脱敏，不含密码）',
      searchPlaceholder: '搜索姓名 / 账号 / 角色',
      searchFields: ['name', 'username', 'role'],
      rows: state.rows,
      empty: '暂无用户',
      pageSize: 10,
      controls: roleSelect,
      extra: ui.h('button', {
        class: 'btn btn-ghost btn-sm', text: '刷新',
        onclick: function () { refresh(el); }
      }),
      columns: [
        { key: 'id', title: 'ID', render: function (u) { return '<span class="cell-strong">' + u.id + '</span>'; } },
        { key: 'name', title: '姓名' },
        { key: 'username', title: '登录账号' },
        {
          key: 'role', title: '角色', render: function (u) {
            return ui.badge(Admin.store.ROLE_LABEL[u.role] || u.role, ROLE_TONE[u.role] || 'muted');
          }
        },
        { key: 'password', title: '密码', render: function () { return '<span style="color:#6E7770;">已脱敏</span>'; } }
      ]
    });

    var host = el.querySelector('#users-body');
    host.innerHTML = '';

    var byRole = {};
    state.rows.forEach(function (u) { byRole[u.role] = (byRole[u.role] || 0) + 1; });
    host.appendChild(ui.h('div', { class: 'grid grid-metrics' },
      ui.metricCard({ title: '用户总数', value: state.rows.length, unit: '人', icon: Admin.layout.ICONS.users, hint: '当前筛选结果' }),
      ui.metricCard({ title: '管理员 / 调度员', value: (byRole.ADMIN || 0) + (byRole.DISPATCHER || 0), unit: '人', icon: Admin.layout.ICONS.info, tone: 'danger', hint: '可派单与核价' }),
      ui.metricCard({ title: '维修工', value: byRole.REPAIRER || 0, unit: '人', icon: Admin.layout.ICONS.wrench, tone: 'warn', hint: '手机端接单执行' }),
      ui.metricCard({ title: '财务', value: byRole.FINANCE || 0, unit: '人', icon: Admin.layout.ICONS.clipboard, tone: 'success', hint: '报销单回填' })
    ));
    host.appendChild(list.node);
  }

  async function refresh(el) {
    var host = el.querySelector('#users-body');
    host.innerHTML = '';
    host.appendChild(Admin.ui.skeletonLines(6));
    await paint(el);
  }

  Admin.views.users = {
    path: '/users',
    title: '用户管理',
    breadcrumb: ['系统与基础数据配置', '用户管理'],

    async render(el) {
      var ui = Admin.ui;
      el.appendChild(ui.h('div', { class: 'page-head' },
        ui.h('h2', { text: '用户管理' }),
        ui.h('span', { class: 'sub', text: '账号与角色（管理员 / 调度员 / 维修工 / 财务）；科室报修免登录，不占账号' })
      ));
      el.appendChild(ui.h('div', { id: 'users-body' }, ui.skeletonLines(6)));
      await paint(el);
    }
  };
})(window);
