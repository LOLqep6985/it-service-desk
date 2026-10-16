/*!
 * 端守卫（共用）：三端账户隔离
 * - 「三端」按账户体系划分：admin.html（管理员/调度/财务）、today.html+detail.html（维修工）、
 *   create.html（报修人）。科室端 report.html 免登录工作机，不在账户体系内。
 * - 规则：登录后角色决定唯一可达端；在错误端打开时【不跳转到其他端】，
 *   提示后强制退出回登录页 —— login.html 是唯一的分发点。
 * - 退出：清 itsdUser 回 login.html。
 */
(function (global) {
  'use strict';

  var ROLE_ENDPOINTS = {
    ADMIN: 'admin',
    DISPATCHER: 'admin',
    FINANCE: 'admin',
    REPAIRER: 'repairer',
    REPORTER: 'reporter'
  };

  function user() {
    try {
      return JSON.parse(localStorage.getItem('itsdUser') || 'null') || {};
    } catch (e) { return {}; }
  }

  function logout() {
    localStorage.removeItem('itsdUser');
    global.location.href = '/login.html';
  }

  /**
   * 守卫当前端。
   * @param endpoint 'admin' | 'repairer' | 'reporter'
   * @returns {object|null} 命中当前端的用户对象；未登录或角色不匹配时已跳转并返回 null
   */
  function guard(endpoint) {
    var u = user();
    if (!u.token) {
      global.location.replace('/login.html');
      return null;
    }
    var allowed = ROLE_ENDPOINTS[u.roleCode];
    if (allowed !== endpoint) {
      alert('当前账户角色「' + (u.role || u.roleCode || '未知') + '」无权访问此端，请用对应角色的账号登录。即将返回登录页。');
      logout();
      return null;
    }
    return u;
  }

  global.ItsdGuard = {
    guard: guard,
    logout: logout,
    user: user
  };
})(window);
