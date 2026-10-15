/* IT 运维工单系统 Service Worker
 * 策略：
 * - 页面导航：网络优先（保证内容最新），失败时回退缓存（离线壳）
 * - 静态资源（图标/manifest）：缓存优先
 * - /api/ 与 /uploads/ 一律不缓存（数据实时）
 */
const CACHE = 'itsd-v14';
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
  '/js/admin/api.js',
  '/js/admin/store.js',
  '/js/admin/ui.js',
  '/js/admin/router.js',
  '/js/admin/layout.js',
  '/js/admin/app.js',
  '/js/admin/views/dashboard.js',
  '/js/admin/views/reports-pending.js',
  '/js/admin/views/work-orders.js',
  '/js/admin/views/reports-history.js',
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
  // 页面导航：网络优先，失败回退缓存
  if (e.request.mode === 'navigate') {
    e.respondWith(
      fetch(e.request)
        .then((res) => {
          const copy = res.clone();
          caches.open(CACHE).then((c) => c.put(e.request, copy));
          return res;
        })
        .catch(() =>
          caches.match(e.request).then((m) => m || caches.match('/report.html'))
        )
    );
    return;
  }
  // 其他静态资源：缓存优先，同时回填缓存
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