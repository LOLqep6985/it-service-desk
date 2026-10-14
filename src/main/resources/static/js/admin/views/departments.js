/*! 视图占位：科室索引管理（批次 3 迁移真实实现） */
(function (global) {
  'use strict';
  var Admin = global.Admin = global.Admin || {};
  Admin.views = Admin.views || {};
  Admin.views.departments = Admin.ui.placeholderRoute({
    path: '/departments',
    title: '科室索引管理',
    breadcrumb: ['系统与基础数据配置', '科室索引管理'],
    note: '批次 3 迁移：院区 → 楼栋 → 楼层 分类浏览（复用 DeptCascade 分组）+ 搜索'
  });
})(window);
