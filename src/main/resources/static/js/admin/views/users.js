/*! 视图占位：用户管理（批次 3 迁移真实实现） */
(function (global) {
  'use strict';
  var Admin = global.Admin = global.Admin || {};
  Admin.views = Admin.views || {};
  Admin.views.users = Admin.ui.placeholderRoute({
    path: '/users',
    title: '用户管理',
    breadcrumb: ['系统与基础数据配置', '用户管理'],
    note: '批次 3 迁移：用户列表（角色标签/脱敏）+ 角色过滤 + 搜索/分页'
  });
})(window);
