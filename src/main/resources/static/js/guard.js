/*!
 * 端守卫（共用）：三端账户隔离 + 浏览器返回键防重入
 * - 「三端」按账户体系划分：admin.html（管理员/调度/财务）、today.html+detail.html（维修工）、
 *   create.html（报修人）。科室端 report.html 免登录工作机，不在账户体系内。
 * - 规则：登录后角色决定唯一可达端；在错误端打开时【不跳转到其他端】，
 *   提示后强制退出回登录页 —— login.html 是唯一的分发点。
 *
 * 退出与「返回键」语义（重要）：
 * - 退出用 location.replace 而非 href：把当前已登录页面**从历史记录中替换掉**，
 *   因此退出后按浏览器返回键不会回到已登录界面（返回键在语义上等同退出登录）。
 * - bfcache（往返缓存）防护：浏览器前进/后退可能直接恢复页面快照、**脚本不重跑**，
 *   此时页面看起来像没退出过。故监听 pageshow.persisted，一旦从缓存恢复就重新校验登录态，
 *   校验失败立即静默跳回登录页。
 * - 服务端对 HTML 下发 Cache-Control: no-store，进一步禁止返回键复用已登录页面缓存。
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

  var restoreWatchInstalled = false;

  function user() {
    try {
      return JSON.parse(localStorage.getItem('itsdUser') || 'null') || {};
    } catch (e) { return {}; }
  }

  /** 退出登录：清凭证 + replace 回登录页（replace 让返回键无法回到已登录页面） */
  function logout() {
    try { localStorage.removeItem('itsdUser'); } catch (e) { /* ignore */ }
    global.location.replace('/login.html');
  }

  /**
   * 校验当前端登录态。
   * @param endpoint 'admin' | 'repairer' | 'reporter'
   * @param silent   true = 静默跳转（bfcache 恢复场景，不弹提示）
   * @returns {object|null} 合法用户对象；否则已跳转并返回 null
   */
  function verify(endpoint, silent) {
    var u = user();
    if (!u.token) {
      // 未登录（含退出后按返回键回到本页）→ 静默回登录页
      global.location.replace('/login.html');
      return null;
    }
    if (ROLE_ENDPOINTS[u.roleCode] !== endpoint) {
      if (!silent) {
        alert('当前账户角色「' + (u.role || u.roleCode || '未知') + '」无权访问此端，请用对应角色的账号登录。即将返回登录页。');
      }
      logout();
      return null;
    }
    return u;
  }

  /**
   * 守卫当前端（页面脚本启动时调用一次即可）。
   * 同时安装 bfcache 恢复监听：浏览器返回键恢复页面快照时再次校验登录态。
   */
  function guard(endpoint) {
    if (!restoreWatchInstalled) {
      restoreWatchInstalled = true;
      global.addEventListener('pageshow', function (e) {
        // persisted=true 表示页面来自往返缓存（返回/前进键），脚本快照不会重跑
        if (e.persisted) {
          verify(endpoint, true);
        }
      });
    }
    return verify(endpoint, false);
  }

  global.ItsdGuard = {
    guard: guard,
    verify: verify,
    logout: logout,
    user: user
  };
})(window);
