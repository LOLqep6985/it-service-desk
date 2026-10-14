/*! 视图占位：系统信息（批次 3 迁移真实实现） */
(function (global) {
  'use strict';
  var Admin = global.Admin = global.Admin || {};
  Admin.views = Admin.views || {};
  Admin.views.system = Admin.ui.placeholderRoute({
    path: '/system',
    title: '系统信息',
    breadcrumb: ['系统与基础数据配置', '系统信息'],
    note: '批次 3 迁移：运行环境（版本/端口/上传目录/数据库）、演示账号、状态机说明、已知限制'
  });
})(window);
