/*! 视图占位：待受理报修单（批次 3 迁移真实实现） */
(function (global) {
  'use strict';
  var Admin = global.Admin = global.Admin || {};
  Admin.views = Admin.views || {};
  Admin.views.reportsPending = Admin.ui.placeholderRoute({
    path: '/reports/pending',
    title: '待受理报修单',
    breadcrumb: ['工单业务管理', '待受理报修单'],
    note: '批次 3 迁移：待受理列表 + 派单弹窗（选择维修工）+ 搜索/分页'
  });
})(window);
