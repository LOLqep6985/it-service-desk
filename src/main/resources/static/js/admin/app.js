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

    if (!Admin.store.isBackendUser()) {
      global.alert('当前账户角色无权访问管理后台，请用管理员/调度员/财务账号登录。即将返回登录页。');
      global.location.href = 'login.html';
      return;
    }

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
