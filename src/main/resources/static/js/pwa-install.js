/*!
 * PWA 安装引导（共用）：report.html / today.html / detail.html / login.html / create.html
 * - Android/Edge/Chrome：捕获 beforeinstallprompt，显示「添加到桌面」按钮，一键安装
 * - iOS Safari/微信：显示手动指引（分享 → 添加到主屏幕），可关闭（localStorage 记忆 7 天）
 * - 已安装（standalone 模式）或已关闭过则不显示
 */
(function (global) {
  'use strict';

  var KEY = 'itsdPwaHintClosedAt';
  var HINT_DAYS = 7;

  function isStandalone() {
    return global.matchMedia && global.matchMedia('(display-mode: standalone)').matches
      || global.navigator.standalone === true;
  }

  function isIOS() {
    return /iphone|ipad|ipod/i.test(global.navigator.userAgent);
  }

  function recentlyClosed() {
    try {
      var t = Number(localStorage.getItem(KEY)) || 0;
      return t > 0 && (Date.now() - t) < HINT_DAYS * 86400 * 1000;
    } catch (e) { return false; }
  }

  function close() {
    try { localStorage.setItem(KEY, String(Date.now())); } catch (e) { /* ignore */ }
    var bar = document.getElementById('itsd-pwa-bar');
    if (bar) bar.remove();
  }

  function bar() {
    var el = document.getElementById('itsd-pwa-bar');
    if (el) return el;
    el = document.createElement('div');
    el.id = 'itsd-pwa-bar';
    el.style.cssText =
      'position:fixed;left:12px;right:12px;bottom:12px;z-index:999;' +
      'display:flex;align-items:center;gap:10px;padding:12px 14px;border-radius:10px;' +
      'background:#1C2420;color:#F3EFE6;font-size:13px;line-height:1.5;' +
      'box-shadow:0 8px 24px rgba(18,24,21,.3);max-width:520px;margin:0 auto;';
    document.body.appendChild(el);
    return el;
  }

  function dismissBtn() {
    var b = document.createElement('button');
    b.textContent = '×';
    b.title = '关闭（7 天内不再提示）';
    b.style.cssText =
      'flex:0 0 auto;margin-left:auto;background:none;border:none;color:#A9B8AF;' +
      'font-size:18px;cursor:pointer;padding:0 4px;';
    b.onclick = close;
    return b;
  }

  function showIosHint() {
    var el = bar();
    var txt = document.createElement('span');
    txt.innerHTML = '把本页装到桌面：点浏览器底部 <b>分享</b> 按钮 → 选择 <b>「添加到主屏幕」</b>，之后从桌面图标进入，无地址栏更像 App。';
    el.appendChild(txt);
    el.appendChild(dismissBtn());
  }

  function showAndroidInstall(prompt) {
    var el = bar();
    var btn = document.createElement('button');
    btn.textContent = '添加到桌面';
    btn.style.cssText =
      'flex:0 0 auto;background:#4ADE9E;color:#0C1A13;border:none;border-radius:8px;' +
      'padding:9px 14px;font-size:13px;font-weight:700;cursor:pointer;';
    btn.onclick = function () {
      prompt.prompt();
      prompt.userChoice.then(function () { close(); });
    };
    var txt = document.createElement('span');
    txt.textContent = '安装到桌面后可全屏使用，无地址栏。';
    el.appendChild(txt);
    el.appendChild(btn);
    el.appendChild(dismissBtn());
  }

  function init() {
    if (isStandalone() || recentlyClosed()) return;
    var deferred = null;
    global.addEventListener('beforeinstallprompt', function (e) {
      e.preventDefault();
      deferred = e;
      showAndroidInstall(deferred);
    });
    // iOS 没有 beforeinstallprompt：页面加载后延迟提示（避免打断首屏）
    if (isIOS()) {
      setTimeout(function () {
        if (!isStandalone() && !recentlyClosed() && !deferred) showIosHint();
      }, 1500);
    }
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
  } else {
    init();
  }
})(window);
