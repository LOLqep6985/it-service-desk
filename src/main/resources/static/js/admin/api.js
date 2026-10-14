/*!
 * 后台 API 封装（Admin.api）
 * 统一处理：X-Token 注入、JSON 序列化、code 校验、401 自动登出、错误消息归一化。
 */
(function (global) {
  'use strict';
  var Admin = global.Admin = global.Admin || {};

  function token() {
    var u = Admin.store && Admin.store.currentUser();
    return u && u.token ? u.token : '';
  }

  /** 把对象拼成 query string（忽略 null/undefined/空串） */
  function qs(params) {
    if (!params) return '';
    var parts = [];
    Object.keys(params).forEach(function (k) {
      var v = params[k];
      if (v === null || v === undefined || v === '') return;
      parts.push(encodeURIComponent(k) + '=' + encodeURIComponent(v));
    });
    return parts.length ? ('?' + parts.join('&')) : '';
  }

  function onAuthError(code, message) {
    if (code === 401) {
      try { localStorage.removeItem('itsdUser'); } catch (e) { /* ignore */ }
      Admin.ui && Admin.ui.toast('登录已过期，请重新登录', 'error');
      setTimeout(function () { global.location.href = 'login.html'; }, 800);
    }
  }

  async function request(path, options) {
    var opts = Object.assign({ method: 'GET' }, options || {});
    opts.headers = Object.assign({}, opts.headers || {});
    var t = token();
    if (t) opts.headers['X-Token'] = t;
    if (opts.body !== undefined && opts.body !== null && typeof opts.body !== 'string') {
      opts.headers['Content-Type'] = 'application/json';
      opts.body = JSON.stringify(opts.body);
    }

    var res;
    try {
      res = await fetch(path, opts);
    } catch (e) {
      throw new Error('网络异常，请检查网络后重试');
    }

    var json;
    try {
      json = await res.json();
    } catch (e) {
      throw new Error('服务端返回异常（HTTP ' + res.status + '）');
    }

    if (json.code === 401 || json.code === 403) {
      onAuthError(json.code, json.message);
      var err = new Error(json.message || '无权限访问');
      err.code = json.code;
      throw err;
    }
    if (json.code !== 0) {
      var e2 = new Error(json.message || '请求失败');
      e2.code = json.code;
      throw e2;
    }
    return json.data;
  }

  Admin.api = {
    qs: qs,
    request: request,
    get: function (path, params) { return request(path + qs(params)); },
    post: function (path, body, params) { return request(path + qs(params), { method: 'POST', body: body }); }
  };
})(window);
