/*! 视图占位：报修单历史（批次 3 迁移真实实现） */
(function (global) {
  'use strict';
  var Admin = global.Admin = global.Admin || {};
  Admin.views = Admin.views || {};
  Admin.views.reportsHistory = Admin.ui.placeholderRoute({
    path: '/reports/history',
    title: '报修单历史',
    breadcrumb: ['工单业务管理', '报修单历史'],
    note: '批次 3 迁移：已受理/已派单/已取消的报修单列表 + 状态过滤 + 搜索/分页'
  });
})(window);
