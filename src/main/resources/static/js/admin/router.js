/*!
 * 后台 hash 路由（Admin.router）
 * 为什么用 hash：Spring Boot 是静态资源直出、没有服务端路由，
 * #/xxx 在刷新、分享、前进后退时都不会 404，零后端改动；
 * 以后接入 Nginx History fallback 也很容易换成 path 模式。
 */
(function (global) {
  'use strict';
  var Admin = global.Admin = global.Admin || {};
  var doc = global.document;

  var routes = [];
  var current = null;      // { route, query, destroy }
  var hooks = {};
  var fallbackPath = '/dashboard';   // 由 start(roleDefaultPath) 设定：财务落地到 /reimbursements

  function register(route) {
    if (!route || !route.path) return;
    routes.push(route);
  }

  function normalize(hash) {
    var raw = String(hash || '').replace(/^#/, '');
    if (!raw || raw === '/') return { path: fallbackPath, query: {} };
    var qIdx = raw.indexOf('?');
    var path = qIdx >= 0 ? raw.slice(0, qIdx) : raw;
    var query = {};
    if (qIdx >= 0) {
      new global.URLSearchParams(raw.slice(qIdx + 1)).forEach(function (v, k) { query[k] = v; });
    }
    if (path.charAt(0) !== '/') path = '/' + path;
    return { path: path, query: query };
  }

  function match(path) {
    for (var i = 0; i < routes.length; i++) {
      if (routes[i].path === path) return routes[i];
    }
    return null;
  }

  function errorView(err, route) {
    var isForbidden = err && err.code === 403;
    return Admin.ui.empty(
      (isForbidden ? '无权限访问该页面：' : '页面加载失败：') + (err && err.message ? err.message : '') ,
      isForbidden ? '🔒' : '⚠️'
    );
  }

  async function render() {
    var loc = normalize(global.location.hash);
    var route = match(loc.path);

    // 未知路由 → 回落到该角色的默认页（财务为 /reimbursements）
    if (!route) {
      if (loc.path !== fallbackPath) {
        global.location.hash = '#' + fallbackPath;
        return;
      }
      return;
    }

    // 离开旧视图：先跑 onLeave，再跑视图自己的清理函数（用于清定时器/订阅）
    if (current) {
      if (typeof current.route.onLeave === 'function') {
        try { current.route.onLeave(); } catch (e) { /* 单个视图异常不影响路由 */ }
      }
      if (typeof current.destroy === 'function') {
        try { current.destroy(); } catch (e) { /* ignore */ }
      }
    }

    current = { route: route, query: loc.query, destroy: null };
    if (hooks.onChange) hooks.onChange(route, loc.query);

    var host = doc.getElementById('content');
    host.innerHTML = '';
    var box = doc.createElement('div');
    host.appendChild(box);

    try {
      var destroy = await route.render(box, { query: loc.query, route: route });
      current.destroy = typeof destroy === 'function' ? destroy : null;
    } catch (e) {
      host.innerHTML = '';
      host.appendChild(errorView(e, route));
    }
    global.scrollTo(0, 0);
  }

  function start(defaultPath) {
    fallbackPath = defaultPath || '/dashboard';
    if (!global.location.hash) {
      global.location.hash = '#' + fallbackPath;
    }
    global.addEventListener('hashchange', render);
    return render();
  }

  function navigate(path, query) {
    var qs = Admin.api ? Admin.api.qs(query) : '';
    global.location.hash = '#' + path + qs;
  }

  function reload() { return render(); }

  Admin.router = {
    register: register,
    start: start,
    navigate: navigate,
    reload: reload,
    onChange: function (fn) { hooks.onChange = fn; },
    current: function () { return current ? current.route : null; },
    routes: function () { return routes.slice(); },
    normalize: normalize
  };
})(window);
