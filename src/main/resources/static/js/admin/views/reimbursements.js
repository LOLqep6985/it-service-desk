/*! 视图占位：月度报销单（批次 3 迁移真实实现） */
(function (global) {
  'use strict';
  var Admin = global.Admin = global.Admin || {};
  Admin.views = Admin.views || {};
  Admin.views.reimbursements = Admin.ui.placeholderRoute({
    path: '/reimbursements',
    title: '月度报销单',
    breadcrumb: ['财务与报销', '月度报销单'],
    note: '批次 3 迁移：按月生成报销单 + 列表 + 详情抽屉（含关联工单）+ 财务回填'
  });
})(window);
