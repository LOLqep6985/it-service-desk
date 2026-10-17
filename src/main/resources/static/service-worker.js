/* IT 运维工单系统 Service Worker
 * 策略：
 * - 页面导航 + 代码类资源（html/js/css/json）：**网络优先**，失败回退缓存
 *   （内网应用几乎总在线，网络优先可保证发版后刷新即生效，避免"改了前端却仍跑旧缓存"）
 * - 图标等极少变动资源：缓存优先
 * - /api/ 与 /uploads/ 一律不缓存（数据实时）
 */
const CACHE = 'itsd-v22';
const APP_SHELL = [
  '/report.html',
  '/today.html',
  '/detail.html',
  '/login.html',
  '/create.html',
  '/admin.html',
  '/css/dept-cascade.css',
  '/css/admin-layout.css',
  '/css/signature-pad.css',
  '/js/department-cascade.js',
  '/js/signature-pad.js',
  '/js/guard.js',
  '/js/notifications.js',
  '/js/admin/api.js',
  '/js/admin/store.js',
  '/js/admin/ui.js',
  '/js/admin/router.js',
  '/js/admin/layout.js',
  '/js/admin/app.js',
  '/js/admin/views/dashboard.js',
  '/js/admin/views/reports-pending.js',
  '/js/admin/views/work-orders.js',
  '/js/admin/views/reimbursements.js',
  '/js/admin/views/departments.js',
  '/js/admin/views/users.js',
  '/js/admin/views/system.js',
  '/manifest-report.json',
  '/manifest-repairer.json',
  '/icons/report-192.png',
  '/icons/report-512.png',
  '/icons/repair-192.png',
  '/icons/repair-512.png'
];

self.addEventListener('install', (e) => {
  e.waitUntil(
    caches.open(CACHE)
      .then((c) => c.addAll(APP_SHELL))
      .then(() => self.skipWaiting())
  );
});

self.addEventListener('activate', (e) => {
  e.waitUntil(
    caches.keys()
      .then((keys) => Promise.all(keys.filter((k) => k !== CACHE).map((k) => caches.delete(k))))
      .then(() => self.clients.claim())
  );
});

self.addEventListener('fetch', (e) => {
  const url = new URL(e.request.url);
  if (e.request.method !== 'GET' || url.origin !== location.origin) {
    return;
  }
  // 接口与上传文件走网络，不拦截
  if (url.pathname.startsWith('/api/') || url.pathname.startsWith('/uploads/')) {
    return;
  }

  const isNavigate = e.request.mode === 'navigate';
  // 代码与页面：网络优先（发版后无需清缓存），断网时回退缓存
  const isCode = isNavigate || /\.(?:html|js|css|json)$/i.test(url.pathname);

  if (isCode) {
    e.respondWith(
      fetch(e.request)
        .then((res) => {
          const copy = res.clone();
          caches.open(CACHE).then((c) => c.put(e.request, copy));
          return res;
        })
        .catch(() =>
          caches.match(e.request).then((m) => m || (isNavigate ? caches.match('/report.html') : undefined))
        )
    );
    return;
  }

  // 图标等极少变动资源：缓存优先，同时回填缓存
  e.respondWith(
    caches.match(e.request).then(
      (m) =>
        m ||
        fetch(e.request).then((res) => {
          const copy = res.clone();
          caches.open(CACHE).then((c) => c.put(e.request, copy));
          return res;
        })
    )
  );
});