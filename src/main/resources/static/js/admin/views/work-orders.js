/*! 视图占位：工单管理（批次 3 迁移真实实现） */
(function (global) {
  'use strict';
  var Admin = global.Admin = global.Admin || {};
  Admin.views = Admin.views || {};
  Admin.views.workOrders = Admin.ui.placeholderRoute({
    path: '/work-orders',
    title: '工单管理',
    breadcrumb: ['工单业务管理', '工单管理'],
    note: '批次 3 迁移：工单列表 + 状态过滤 + 详情抽屉（报修信息/照片/流转/维修/耗材核价/签字图）'
  });
})(window);
