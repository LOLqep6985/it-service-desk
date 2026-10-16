/*!
 * 后台布局（Admin.layout）：深色侧边栏多级菜单 + 顶部导航
 * - 菜单树在此集中定义（视图只提供 path/title/render）
 * - 角标随 Admin.store.badges 自动刷新
 * - 桌面可折叠（记忆到 localStorage），<768px 变抽屉 + 遮罩
 * - 图标为内联 SVG，不依赖任何 CDN/图标字体
 */
(function (global) {
  'use strict';
  var Admin = global.Admin = global.Admin || {};
  var ui = null;   // 延迟取，保证脚本顺序无关
  var doc = global.document;

  function svg(inner, size) {
    var s = size || 16;
    return '<svg width="' + s + '" height="' + s + '" viewBox="0 0 24 24" fill="none" ' +
      'stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">' + inner + '</svg>';
  }

  var ICONS = {
    dashboard: svg('<path d="M3 13a9 9 0 0 1 18 0"/><path d="M12 13l4.5-4.5"/>'),
    clipboard: svg('<rect x="6" y="4" width="12" height="17" rx="2"/><path d="M9 4V3h6v1"/><path d="M9 10h6M9 14h6"/>'),
    wrench: svg('<path d="M14.7 6.3a4 4 0 1 0 5 5L21 21H3l8.7-8.7a4 4 0 0 1 3-6z"/>'),
    history: svg('<circle cx="12" cy="12" r="9"/><path d="M12 7v5l3 2"/>'),
    wallet: svg('<rect x="3" y="6" width="18" height="13" rx="2"/><path d="M16 12h3"/><path d="M3 10h18"/>'),
    building: svg('<rect x="4" y="3" width="16" height="18" rx="2"/><path d="M9 7h2M13 7h2M9 11h2M13 11h2M9 15h2M13 15h2"/>'),
    users: svg('<circle cx="9" cy="8" r="3"/><path d="M3 20a6 6 0 0 1 12 0"/><path d="M16 6a3 3 0 0 1 0 6"/><path d="M18 20a5 5 0 0 0-2-4"/>'),
    info: svg('<circle cx="12" cy="12" r="9"/><path d="M12 11v5M12 8h.01"/>'),
    refresh: svg('<path d="M20 12a8 8 0 1 1-2.6-5.9"/><path d="M20 4v5h-5"/>'),
    menu: svg('<path d="M4 7h16M4 12h16M4 17h16"/>', 20),
    logout: svg('<path d="M15 12H4"/><path d="M8 8l-4 4 4 4"/><path d="M12 4h6a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2h-6"/>'),
    external: svg('<path d="M14 4h6v6"/><path d="M20 4l-9 9"/><path d="M19 14v5a1 1 0 0 1-1 1H5a1 1 0 0 1-1-1V6a1 1 0 0 1 1-1h5"/>'),
    chevron: svg('<path d="M9 6l6 6-6 6"/>', 14)
  };

  /**
   * 菜单树（一级 = 分组，二级 = 路由）
   * roles：可见该菜单的角色；缺省表示所有后台角色可见。
   * 财务只能看到「财务与报销」——菜单按角色渲染，越权路径后端仍会 403。
   */
  var MENU = [
    {
      key: 'workbench', title: '工作台', icon: 'dashboard', roles: ['ADMIN', 'DISPATCHER'],
      items: [{ path: '/dashboard', title: '首页概览', icon: 'dashboard' }]
    },
    {
      key: 'orders', title: '工单业务管理', icon: 'clipboard', roles: ['ADMIN', 'DISPATCHER'],
      items: [
        { path: '/reports/pending', title: '待受理报修单', icon: 'clipboard', badge: 'pendingReports', badgeTone: 'tone-danger' },
        { path: '/work-orders', title: '工单管理', icon: 'wrench', badge: 'activeWork', badgeTone: 'tone-warn' }
      ]
    },
    {
      key: 'finance', title: '财务与报销', icon: 'wallet', roles: ['ADMIN', 'DISPATCHER', 'FINANCE'],
      items: [{ path: '/reimbursements', title: '月度报销单', icon: 'wallet', badge: 'pendingSign', badgeTone: 'tone-primary' }]
    },
    {
      key: 'system', title: '系统与基础数据配置', icon: 'building', roles: ['ADMIN', 'DISPATCHER'],
      items: [
        { path: '/departments', title: '科室索引管理', icon: 'building' },
        { path: '/users', title: '用户管理', icon: 'users' },
        { path: '/system', title: '系统信息', icon: 'info' }
      ]
    }
  ];

  /** 当前角色可见的菜单（分组内项目全被过滤掉则整组隐藏） */
  function visibleMenu() {
    var role = Admin.store.roleCode();
    return MENU.map(function (group) {
      var items = group.items.filter(function (item) {
        return !item.roles || item.roles.indexOf(role) >= 0;
      });
      if (!items.length) return null;
      if (group.roles && group.roles.indexOf(role) < 0) return null;
      return Object.assign({}, group, { items: items });
    }).filter(Boolean);
  }

  /** 登录后的默认落地页（财务没有工作台，直接进月度报销单） */
  function defaultPath() {
    return Admin.store.roleCode() === 'FINANCE' ? '/reimbursements' : '/dashboard';
  }

  var els = {};
  var state = { collapsed: false, mobileOpen: false, activePath: '', badges: {} };
  var COLLAPSE_KEY = 'itsdAdminSidebarCollapsed';

  function isMobile() { return global.innerWidth <= 768; }

  function buildSidebar() {
    var nav = ui.h('nav', { class: 'sidebar-nav' });
    visibleMenu().forEach(function (group) {
      var items = ui.h('div', { class: 'menu-items' }, group.items.map(function (item) {
        return ui.h('div', {
          class: 'menu-item', dataset: { path: item.path }, title: item.title,
          onclick: function () { navigate(item.path); }
        },
          ui.h('span', { class: 'm-icon', html: ICONS[item.icon] || '' }),
          ui.h('span', { class: 'm-text', text: item.title }),
          ui.h('span', { class: 'menu-badge ' + (item.badgeTone || ''), dataset: { badge: item.badge || '' }, hidden: true })
        );
      }));
      var groupEl = ui.h('div', { class: 'menu-group', dataset: { group: group.key } },
        ui.h('div', {
          class: 'menu-group-title',
          onclick: function () { groupEl.classList.toggle('collapsed'); }
        },
          ui.h('span', { class: 'g-icon', html: ICONS[group.icon] || '' }),
          ui.h('span', { class: 'g-label', text: group.title }),
          ui.h('span', { class: 'chev', html: ICONS.chevron })
        ),
        items
      );
      nav.appendChild(groupEl);
    });
    return nav;
  }

  /** 顶栏内容（返回节点数组，挂载时追加到已有的 .topbar 容器） */
  function buildTopbar() {
    var user = Admin.store.currentUser() || {};
    var initial = String(user.name || '?').slice(0, 1);
    return [
      ui.h('button', {
        class: 'hamburger', html: ICONS.menu, title: '展开/收起菜单',
        onclick: function () { if (isMobile()) toggleMobile(); else toggleCollapse(); }
      }),
      ui.h('div', { class: 'breadcrumb' },
        ui.h('span', { text: '管理后台' }),
        ui.h('span', { class: 'sep', text: '/' }),
        ui.h('span', { class: 'current', text: '首页概览' })
      ),
      ui.h('div', { class: 'topbar-actions' },
        ui.h('button', {
          class: 'btn btn-ghost btn-sm', html: ICONS.refresh, title: '刷新当前页',
          onclick: function () { Admin.router.reload(); }
        }),
        // 注：不提供跳往科室端/维修端的入口 —— 三端之间不可达，仅登录页可按角色分发
        ui.h('div', { class: 'user-chip' },
          ui.h('span', { class: 'avatar', text: initial }),
          ui.h('div', null,
            ui.h('div', { class: 'u-name', text: user.name || '未登录' }),
            ui.h('div', { class: 'u-role', text: Admin.store.roleLabel() })
          )
        ),
        ui.h('button', {
          class: 'btn btn-ghost btn-sm', html: ICONS.logout, title: '退出登录',
          onclick: function () { Admin.store.logout(); }
        })
      )
    ];
  }

  function navigate(path) {
    if (isMobile()) closeMobile();
    if (Admin.router.current() && Admin.router.current().path === path) return;
    Admin.router.navigate(path);
  }

  function mount() {
    ui = Admin.ui;
    var sidebar = doc.getElementById('sidebar');
    var topbar = doc.getElementById('topbar');

    sidebar.innerHTML = '';
    sidebar.appendChild(ui.h('div', { class: 'sidebar-brand' },
      ui.h('span', { class: 'logo', text: 'IT' }),
      ui.h('span', { class: 'brand-text', text: '运维工单后台' })
    ));
    sidebar.appendChild(buildSidebar());

    topbar.innerHTML = '';
    buildTopbar().forEach(function (node) { topbar.appendChild(node); });

    renderRail();

    // 折叠：桌面记忆；移动端走抽屉
    var layout = doc.getElementById('layout');
    var saved = null;
    try { saved = localStorage.getItem(COLLAPSE_KEY); } catch (e) { /* ignore */ }
    state.collapsed = saved === '1';
    layout.classList.toggle('collapsed', state.collapsed && !isMobile());

    var overlay = doc.getElementById('sidebar-overlay');
    if (overlay) overlay.addEventListener('click', closeMobile);
    global.addEventListener('resize', function () {
      layout.classList.toggle('collapsed', state.collapsed && !isMobile());
      if (!isMobile()) closeMobile();
    });

    // 角标订阅：store 角标变化时刷新侧栏与右栏
    Admin.store.subscribe(function () { applyBadges(Admin.store.badges()); renderRail(); });
  }

  /** 右栏「今日概况」已随通栏布局移除（admin.html 不再有 #side-rail）；保留空实现避免引用报错 */
  function renderRail() {}

  function toggleCollapse() {
    var layout = doc.getElementById('layout');
    state.collapsed = !state.collapsed;
    layout.classList.toggle('collapsed', state.collapsed);
    try { localStorage.setItem(COLLAPSE_KEY, state.collapsed ? '1' : '0'); } catch (e) { /* ignore */ }
  }

  function openMobile() {
    state.mobileOpen = true;
    doc.getElementById('layout').classList.add('mobile-open');
  }
  function closeMobile() {
    state.mobileOpen = false;
    doc.getElementById('layout').classList.remove('mobile-open');
  }
  function toggleMobile() { state.mobileOpen ? closeMobile() : openMobile(); }

  function setActive(path) {
    state.activePath = path;
    var items = doc.querySelectorAll('.menu-item');
    Array.prototype.forEach.call(items, function (el) {
      var on = el.getAttribute('data-path') === path;
      el.classList.toggle('active', on);
      if (on) {
        var group = el.closest('.menu-group');
        if (group) group.classList.remove('collapsed');
      }
    });
  }

  function setBreadcrumb(parts) {
    var el = doc.querySelector('.breadcrumb');
    if (!el) return;
    var list = (parts && parts.length) ? parts : ['工作台', '首页概览'];
    el.innerHTML = '';
    el.appendChild(ui.h('span', { text: '管理后台' }));
    list.forEach(function (p, i) {
      el.appendChild(ui.h('span', { class: 'sep', text: '/' }));
      el.appendChild(ui.h('span', i === list.length - 1 ? { class: 'current', text: p } : { text: p }));
    });
  }

  function applyBadges(badges) {
    state.badges = badges || {};
    Array.prototype.forEach.call(doc.querySelectorAll('.menu-badge'), function (el) {
      var key = el.getAttribute('data-badge');
      var v = key ? state.badges[key] : 0;
      if (v && v > 0) {
        el.textContent = v > 99 ? '99+' : String(v);
        el.hidden = false;
      } else {
        el.hidden = true;
      }
    });
  }

  Admin.layout = {
    MENU: MENU,
    ICONS: ICONS,
    visibleMenu: visibleMenu,
    defaultPath: defaultPath,
    mount: mount,
    setActive: setActive,
    setBreadcrumb: setBreadcrumb,
    applyBadges: applyBadges,
    toggleCollapse: toggleCollapse,
    openMobile: openMobile,
    closeMobile: closeMobile,
    isMobile: isMobile,
    state: state
  };
})(window);
