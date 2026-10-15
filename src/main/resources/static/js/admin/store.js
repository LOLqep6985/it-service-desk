/*!
 * 后台轻量状态（Admin.store）
 * 职责：登录用户、侧栏角标、科室/用户基础数据缓存、订阅通知。
 * 不引入任何状态管理库——后台数据量小，缓存 + 订阅足够。
 */
(function (global) {
  'use strict';
  var Admin = global.Admin = global.Admin || {};

  var ROLE_LABEL = { ADMIN: '管理员', DISPATCHER: '调度员', REPAIRER: '维修工', REPORTER: '报修人', FINANCE: '财务' };

  var state = {
    user: null,
    badges: {},          // pendingReports / activeWork / pendingSign ...
    departments: null,   // 缓存，避免每个视图重复拉取
    users: null
  };
  var listeners = [];

  function init() {
    var saved = null;
    try { saved = localStorage.getItem('itsdUser'); } catch (e) { /* ignore */ }
    state.user = saved ? JSON.parse(saved) : null;
    return state.user;
  }

  function currentUser() { return state.user; }
  function roleCode() { return (state.user && state.user.roleCode) || ''; }
  /** 运维侧（管理员/调度员）：可见工单业务与系统配置 */
  function isAdminLike() {
    var r = roleCode();
    return r === 'ADMIN' || r === 'DISPATCHER';
  }
  /** 可进入后台的角色：运维 + 财务（财务只能看财务与报销） */
  function isBackendUser() {
    return isAdminLike() || roleCode() === 'FINANCE';
  }
  /** 概览统计接口（/api/stats/dashboard）只对运维开放 */
  function canViewStats() { return isAdminLike(); }
  function roleLabel() {
    var u = state.user;
    return (u && (u.role || ROLE_LABEL[u.roleCode])) || '';
  }
  function logout() {
    try { localStorage.removeItem('itsdUser'); } catch (e) { /* ignore */ }
    global.location.href = 'login.html';
  }

  function setBadges(badges) {
    state.badges = Object.assign({}, state.badges, badges || {});
    notify();
  }
  function badges() { return state.badges; }

  function subscribe(fn) {
    listeners.push(fn);
    return function unsubscribe() {
      var i = listeners.indexOf(fn);
      if (i >= 0) listeners.splice(i, 1);
    };
  }
  function notify() {
    listeners.forEach(function (fn) {
      try { fn(state); } catch (e) { /* 单个订阅异常不影响其它 */ }
    });
  }

  /** 基础数据缓存：默认命中缓存，force=true 时重新拉取 */
  async function loadDepartments(force) {
    if (!force && state.departments) return state.departments;
    state.departments = await Admin.api.get('/api/departments');
    return state.departments;
  }
  async function loadUsers(force) {
    if (!force && state.users) return state.users;
    state.users = await Admin.api.get('/api/users');
    return state.users;
  }
  function userMap() {
    var map = {};
    (state.users || []).forEach(function (u) { map[u.id] = u; });
    return map;
  }
  function clearCache() {
    state.departments = null;
    state.users = null;
  }

  Admin.store = {
    init: init,
    currentUser: currentUser,
    roleCode: roleCode,
    isAdminLike: isAdminLike,
    isBackendUser: isBackendUser,
    canViewStats: canViewStats,
    roleLabel: roleLabel,
    logout: logout,
    setBadges: setBadges,
    badges: badges,
    subscribe: subscribe,
    loadDepartments: loadDepartments,
    loadUsers: loadUsers,
    userMap: userMap,
    clearCache: clearCache,
    ROLE_LABEL: ROLE_LABEL
  };
})(window);
