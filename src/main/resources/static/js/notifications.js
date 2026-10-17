/*!
 * 站内消息 · 前台强提醒（A 档）
 *
 * 设计（findings.md#D-030）：
 *  - 轮询 10s，页面重新可见/获焦时立即补拉；服务端是唯一事实来源，断网切后台都不丢
 *  - URGENT 消息响铃 + 震动；NORMAL 只进横幅
 *  - 标题加未读数角标；已读只用于消自己的红点，**不做已读回执给管理员**
 *  - 不做夜间免打扰（非工作时间不考虑）
 *
 * 身份自动识别：
 *  - localStorage.itsdUser 有 token        → 登录用户（管理员/调度/维修工）
 *  - 否则 localStorage.itsdDeviceId 有值    → 科室端设备
 *  - 两者都没有 → 不轮询
 */
(function (global) {
  'use strict';

  var POLL_MS = 10000;
  var state = { kind: '', lastId: 0, unread: 0, started: false, audioCtx: null, onNew: null };
  var baseTitle = document.title;

  function token() {
    try {
      var u = JSON.parse(localStorage.getItem('itsdUser') || 'null');
      return u && u.token ? u.token : '';
    } catch (e) { return ''; }
  }

  function deviceId() {
    try { return localStorage.getItem('itsdDeviceId') || ''; } catch (e) { return ''; }
  }

  function sinceKey() {
    return 'itsdNoticeSince:' + state.kind + ':' + (state.kind === 'device' ? deviceId() : token());
  }
  function loadSince() {
    try { return Number(localStorage.getItem(sinceKey())) || 0; } catch (e) { return 0; }
  }
  function saveSince(v) {
    try { localStorage.setItem(sinceKey(), String(v)); } catch (e) { /* ignore */ }
  }

  // ---------- 响铃 / 震动（浏览器要求先有用户手势才允许出声）----------
  function ensureAudio() {
    if (state.audioCtx) return state.audioCtx;
    try {
      var Ctx = global.AudioContext || global.webkitAudioContext;
      if (Ctx) state.audioCtx = new Ctx();
    } catch (e) { /* 不支持就静默 */ }
    return state.audioCtx;
  }

  function beep(times) {
    var ctx = ensureAudio();
    if (!ctx) return;
    try { if (ctx.state === 'suspended') ctx.resume(); } catch (e) { }
    var n = Math.max(1, Math.min(times || 1, 3));
    for (var i = 0; i < n; i++) {
      var t0 = ctx.currentTime + i * 0.3;
      var osc = ctx.createOscillator();
      var gain = ctx.createGain();
      osc.type = 'sine';
      osc.frequency.value = 880;
      gain.gain.setValueAtTime(0.0001, t0);
      gain.gain.exponentialRampToValueAtTime(0.25, t0 + 0.02);
      gain.gain.exponentialRampToValueAtTime(0.0001, t0 + 0.22);
      osc.connect(gain);
      gain.connect(ctx.destination);
      osc.start(t0);
      osc.stop(t0 + 0.24);
    }
  }

  function vibrate(pattern) {
    try { if (navigator.vibrate) navigator.vibrate(pattern); } catch (e) { /* iOS 不支持 */ }
  }

  // ---------- 视觉 ----------
  function paintBadge() {
    document.title = state.unread > 0 ? '(' + state.unread + ') ' + baseTitle : baseTitle;
  }

  function banner(items) {
    var urgent = items.filter(function (n) { return n.level === 'URGENT'; });
    var show = urgent.length ? urgent : items;
    var last = show[show.length - 1];
    var el = document.getElementById('itsd-notice-bar');
    if (!el) {
      el = document.createElement('div');
      el.id = 'itsd-notice-bar';
      el.style.cssText = 'position:fixed;left:12px;right:12px;bottom:12px;z-index:9999;display:flex;gap:10px;'
        + 'align-items:center;padding:12px 14px;border-radius:10px;background:#7A3B12;color:#FFF6EC;'
        + 'font-size:13px;line-height:1.5;box-shadow:0 8px 24px rgba(0,0,0,.3);max-width:560px;margin:0 auto;cursor:pointer;';
      document.body.appendChild(el);
    }
    el.innerHTML = '';
    var txt = document.createElement('span');
    txt.textContent = show.length > 1
      ? ('你有 ' + show.length + ' 条新消息：' + last.title)
      : last.title;
    el.appendChild(txt);
    var close = document.createElement('button');
    close.textContent = '×';
    close.style.cssText = 'flex:0 0 auto;margin-left:auto;background:none;border:none;color:#F3D7B8;font-size:18px;cursor:pointer;';
    close.onclick = function (e) { e.stopPropagation(); el.remove(); };
    el.appendChild(close);
    el.onclick = function () {
      el.remove();
      markRead(show.map(function (n) { return n.id; }));
    };
    clearTimeout(banner._t);
    banner._t = setTimeout(function () { if (el.parentNode) el.remove(); }, 30000);
  }

  // ---------- 网络 ----------
  function headers() {
    var h = {};
    if (state.kind === 'user') h['X-Token'] = token();
    return h;
  }

  function query() {
    var parts = ['since=' + encodeURIComponent(state.lastId || 0), 'limit=20'];
    if (state.kind === 'device') parts.push('deviceCode=' + encodeURIComponent(deviceId()));
    return '?' + parts.join('&');
  }

  function readUrl() {
    return '/api/notifications/read'
      + (state.kind === 'device' ? '?deviceCode=' + encodeURIComponent(deviceId()) : '');
  }

  async function markRead(ids) {
    if (!ids || !ids.length) return;
    try {
      await fetch(readUrl(), {
        method: 'POST',
        headers: Object.assign({ 'Content-Type': 'application/json' }, headers()),
        body: JSON.stringify({ ids: ids })
      });
      state.unread = Math.max(0, state.unread - ids.length);
      paintBadge();
    } catch (e) { /* ignore */ }
  }

  async function poll() {
    if (!state.started) return;
    try {
      var res = await fetch('/api/notifications' + query(), { headers: headers() });
      var json = await res.json();
      if (!json || json.code !== 0) return;
      var data = json.data || {};
      var items = data.items || [];
      state.unread = data.unread || 0;
      paintBadge();

      if (items.length) {
        state.lastId = data.maxId || state.lastId;
        saveSince(state.lastId);
        var urgent = items.filter(function (n) { return n.level === 'URGENT'; });
        if (urgent.length) {
          beep(urgent.length > 1 ? 2 : 1);
          vibrate([120, 60, 120]);
        }
        banner(items);
        if (typeof state.onNew === 'function') state.onNew(items);
      } else if (data.maxId && data.maxId > state.lastId) {
        state.lastId = data.maxId;
        saveSince(state.lastId);
      }
    } catch (e) { /* 网络抖动：下一轮再来 */ }
  }

  function start(opts) {
    if (state.started) return false;
    opts = opts || {};
    if (token()) {
      state.kind = 'user';
    } else if (deviceId()) {
      state.kind = 'device';
    } else {
      return false;
    }
    state.onNew = opts.onNew || null;
    state.lastId = loadSince();
    state.started = true;

    // 首次点击/触摸解锁音频（浏览器自动播放策略）
    global.addEventListener('click', function unlock() { ensureAudio(); }, { once: true });
    global.addEventListener('touchstart', function unlock2() { ensureAudio(); }, { once: true });

    poll();
    setInterval(poll, opts.interval || POLL_MS);
    document.addEventListener('visibilitychange', function () { if (!document.hidden) poll(); });
    global.addEventListener('focus', poll);
    return true;
  }

  global.ITSNotice = { start: start, poll: poll, beep: beep };
})(window);
