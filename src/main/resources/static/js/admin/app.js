/*!
 * 后台启动引导（Admin.app）
 * 流程：登录校验 → 渲染布局 → 注册路由 → 预取角标 → 启动 hash 路由
 */
(function (global) {
  'use strict';
  var Admin = global.Admin = global.Admin || {};
  var doc = global.document;

  /** 侧栏角标：取自概览统计接口；财务无该权限、失败也不阻塞页面 */
  Admin.refreshBadges = async function () {
    if (!Admin.store.canViewStats || !Admin.store.canViewStats()) return null;
    try {
      var s = await Admin.api.get('/api/stats/dashboard');
      Admin.store.setBadges({
        pendingReports: s.pendingReports || 0,
        activeWork: s.activeWorkOrders || 0,
        pendingSign: s.monthReimbursementPendingSign || 0
      });
      return s;
    } catch (e) {
      return null;
    }
  };

  async function boot() {
    Admin.store.init();

    var user = Admin.store.currentUser();
    if (!user || !user.token) {
      // 未登录（含退出后按浏览器返回键回到本页）→ 静默回登录页，不弹提示
      global.location.replace('login.html');
      return;
    }
    if (!Admin.store.isBackendUser()) {
      global.alert('当前账户角色无权访问管理后台，请用管理员/调度员/财务账号登录。即将返回登录页。');
      global.location.replace('login.html');
      return;
    }

    // 浏览器返回/前进键：页面可能从往返缓存(bfcache)恢复且脚本不重跑 —— 重新校验登录态
    global.addEventListener('pageshow', function (e) {
      if (!e.persisted) return;
      Admin.store.init();
      var u = Admin.store.currentUser();
      if (!u || !u.token || !Admin.store.isBackendUser()) {
        global.location.replace('login.html');
      }
    });

    // 跨标签页身份看守：localStorage 是同浏览器全窗口共享的，另一个窗口登录别的账号会
    // 静默覆盖本窗口身份（2026-10-16「点生成报销单后变成财务界面」的根因）。
    // 检测到身份被替换时明确提示并刷新，而不是让用户莫名其妙地换了个角色。
    var bootUserId = user.userId;
    global.addEventListener('storage', function (e) {
      if (e.key !== 'itsdUser') return;
      var next = null;
      try { next = e.newValue ? JSON.parse(e.newValue) : null; } catch (err) { /* ignore */ }
      if (!next || !next.token || next.userId !== bootUserId) {
        global.alert('检测到本浏览器在其它窗口登录了其他账号，当前页面身份已失效，即将返回登录页。');
        global.location.replace('login.html');
      }
    });

    // 注册各视图路由（视图文件自行挂到 Admin.views，新增视图无需改本文件）
    var views = Admin.views || {};
    Object.keys(views).forEach(function (key) { Admin.router.register(views[key]); });

    Admin.layout.mount();

    Admin.router.onChange(function (route) {
      Admin.layout.setActive(route.path);
      Admin.layout.setBreadcrumb(route.breadcrumb);
      doc.title = (route.title ? route.title + ' · ' : '') + 'IT 运维工单 · 管理后台';
    });

    doc.getElementById('app-loading').hidden = true;
    doc.getElementById('layout').hidden = false;

    Admin.refreshBadges();
    // 财务没有工作台：默认落地到月度报销单
    await Admin.router.start(Admin.layout.defaultPath());
  }

  if (doc.readyState === 'loading') {
    doc.addEventListener('DOMContentLoaded', boot);
  } else {
    boot();
  }
})(window);
