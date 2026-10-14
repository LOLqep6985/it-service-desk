/*!
 * 视图：系统与基础数据配置 · 系统信息（#/system）
 * 内容：运行环境（来自 /api/system/info）、数据规模、演示账号、业务状态机、已知限制
 * 说明：「已知限制」与 findings.md#D-003 保持一致，对使用方透明。
 */
(function (global) {
  'use strict';
  var Admin = global.Admin = global.Admin || {};
  Admin.views = Admin.views || {};

  var DEMO_ACCOUNTS = [
    ['管理员', 'admin', 'admin123', '派单 / 核价 / 报销单管理'],
    ['调度员', 'dispatcher', '123456', '与管理员同权限（现有实现）'],
    ['维修工', 'repairer1', '123456', '手机端接单与维修记录（张工）'],
    ['维修工', 'repairer2', '123456', '手机端接单与维修记录（李工）'],
    ['报修人', 'reporter1', '123456', 'PC 登录式报修入口（王护士）']
  ];

  var KNOWN_LIMITS = [
    ['报销单签字留痕', '报销单签字仅记录签字人与时间，手写签名图片未落库（工单验收签字正常）', 'warn'],
    ['维修端状态显示', 'today.html / detail.html 的状态映射缺少「待验收」，个别状态会显示英文原名', 'warn'],
    ['防重复提交口径', '10 分钟防重按「科室 + 设备名」判定，而设备名已改为下拉宽泛类型，可能误判', 'warn'],
    ['工单详情权限', '已登录用户可查看任意工单详情，未按归属人校验', 'muted'],
    ['密码存储', '演示环境密码明文存储，生产环境需改为哈希（BCrypt）', 'muted']
  ];

  function kvRows(rows) {
    var ui = Admin.ui;
    return rows.map(function (r) {
      return ui.h('div', { class: 'detail-row' },
        ui.h('span', { class: 'k', text: r[0] }),
        ui.h('span', { class: 'v' }, r[1] instanceof global.Node ? r[1] : String(r[1] === null || r[1] === undefined || r[1] === '' ? '-' : r[1]))
      );
    });
  }

  function fmtUptime(minutes) {
    var m = Number(minutes || 0);
    if (m < 60) return m + ' 分钟';
    var h = Math.floor(m / 60);
    if (h < 24) return h + ' 小时 ' + (m % 60) + ' 分钟';
    return Math.floor(h / 24) + ' 天 ' + (h % 24) + ' 小时';
  }

  async function paint(el) {
    var ui = Admin.ui;
    var info = await Admin.api.get('/api/system/info');
    var c = info.counts || {};
    var host = el.querySelector('#system-body');
    host.innerHTML = '';

    host.appendChild(ui.h('div', { class: 'grid grid-metrics', style: { marginBottom: '16px' } },
      ui.metricCard({ title: '应用版本', value: info.version, icon: Admin.layout.ICONS.info, hint: info.application }),
      ui.metricCard({ title: '运行时长', value: fmtUptime(info.uptimeMinutes), icon: Admin.layout.ICONS.history, tone: 'success', hint: '启动于 ' + info.startedAt }),
      ui.metricCard({ title: '工单总数', value: c.workOrders || 0, unit: '单', icon: Admin.layout.ICONS.wrench, hint: '已完成 ' + (c.completedWorkOrders || 0) + ' 单' }),
      ui.metricCard({ title: '报修单总数', value: c.reports || 0, unit: '单', icon: Admin.layout.ICONS.clipboard, tone: 'warn', hint: '待受理 ' + (c.pendingReports || 0) + ' 单' })
    ));

    host.appendChild(ui.h('div', { class: 'grid grid-2' },
      ui.card({
        title: '运行环境', body: ui.h('div', null, kvRows([
          ['应用名称', info.application],
          ['版本', info.version],
          ['服务端口', info.serverPort],
          ['Java', info.javaVersion + '（' + (info.javaVendor || '') + '）'],
          ['操作系统', info.os],
          ['启动时间', info.startedAt],
          ['上传目录', info.uploadDir],
          ['数据库地址', info.dbUrl],
          ['数据库账号', info.dbUsername]
        ]))
      }),
      ui.card({
        title: '数据规模', body: ui.h('div', null, kvRows([
          ['用户', (c.users || 0) + ' 人'],
          ['科室', (c.departments || 0) + ' 个'],
          ['报修单', (c.reports || 0) + ' 单（待受理 ' + (c.pendingReports || 0) + '）'],
          ['工单', (c.workOrders || 0) + ' 单（已完成 ' + (c.completedWorkOrders || 0) + '）'],
          ['报销闭环', '签字 → 按月报销 → 财务回填'],
          ['数据初始化', 'schema.sql + data.sql 幂等执行（重启不丢数据）']
        ]))
      })
    ));

    host.appendChild(ui.card({
      title: '演示账号',
      hint: '（仅演示环境，生产环境请修改）',
      body: ui.dataTable({
        columns: [
          { key: 'role', title: '角色' },
          { key: 'username', title: '账号', render: function (a) { return '<span class="cell-strong">' + ui.esc(a.username) + '</span>'; } },
          { key: 'password', title: '密码' },
          { key: 'note', title: '用途' }
        ],
        rows: DEMO_ACCOUNTS.map(function (a) { return { role: a[0], username: a[1], password: a[2], note: a[3] }; }),
        empty: '暂无'
      })
    }));

    host.appendChild(ui.h('div', { class: 'grid grid-2' },
      ui.card({
        title: '业务状态机',
        body: ui.h('div', null,
          ui.h('div', { class: 'detail-section' },
            ui.h('h4', { text: '报修单' }),
            ui.h('div', { class: 'detail-row' }, ui.h('span', { class: 'v' },
              ui.statusBadge(ui.STATUS.REPORT, 'PENDING'), ' → ', ui.statusBadge(ui.STATUS.REPORT, 'DISPATCHED'),
              ui.h('span', { style: { color: '#7f8c8d', marginLeft: '8px' }, text: '（派单即生成工单；取消为终态）' })))
          ),
          ui.h('div', { class: 'detail-section' },
            ui.h('h4', { text: '工单' }),
            ui.h('div', { class: 'detail-row' }, ui.h('span', { class: 'v' },
              ui.statusBadge(ui.STATUS.WORK_ORDER, 'ASSIGNED'), ' → ',
              ui.statusBadge(ui.STATUS.WORK_ORDER, 'IN_PROGRESS'), ' → ',
              ui.statusBadge(ui.STATUS.WORK_ORDER, 'PENDING_ACCEPT'), ' → ',
              ui.statusBadge(ui.STATUS.WORK_ORDER, 'COMPLETED')))
          ),
          ui.h('div', { class: 'detail-section' },
            ui.h('h4', { text: '报销单' }),
            ui.h('div', { class: 'detail-row' }, ui.h('span', { class: 'v' },
              ui.statusBadge(ui.STATUS.REIMBURSEMENT, 'PENDING_SIGN'), ' → ',
              ui.statusBadge(ui.STATUS.REIMBURSEMENT, 'PENDING_REIMBURSE'), ' → ',
              ui.statusBadge(ui.STATUS.REIMBURSEMENT, 'REIMBURSED'))))
        )
      }),
      ui.card({
        title: '已知限制与后续优化',
        hint: '（对使用方透明）',
        body: ui.h('div', null, KNOWN_LIMITS.map(function (l) {
          return ui.h('div', { class: 'detail-row' },
            ui.h('span', { class: 'k', text: l[0] }),
            ui.h('span', { class: 'v' }, ui.badge(l[2] === 'warn' ? '待优化' : '已知', l[2]), ' ' + l[1])
          );
        }))
      })
    ));

    host.appendChild(ui.card({
      title: '前端模块',
      hint: '（无构建步骤，纯静态）',
      body: ui.h('div', null, kvRows([
        ['后台结构', '单入口 admin.html + hash 路由 + 视图模块（js/admin/**）'],
        ['路由', (Admin.router.routes() || []).map(function (r) { return '#' + r.path; }).join('  ')],
        ['离线能力', 'PWA：Service Worker 预缓存静态资源（navigation 网络优先）'],
        ['第三方依赖', '无（图标内联 SVG，图表纯 CSS）']
      ]))
    }));
  }

  Admin.views.system = {
    path: '/system',
    title: '系统信息',
    breadcrumb: ['系统与基础数据配置', '系统信息'],

    async render(el) {
      var ui = Admin.ui;
      el.appendChild(ui.h('div', { class: 'page-head' },
        ui.h('h2', { text: '系统信息' }),
        ui.h('span', { class: 'sub', text: '运行环境、数据规模、演示账号、状态机与已知限制' })
      ));
      el.appendChild(ui.h('div', { id: 'system-body' }, ui.skeletonLines(8)));
      try {
        await paint(el);
      } catch (e) {
        el.querySelector('#system-body').innerHTML = '';
        el.querySelector('#system-body').appendChild(ui.empty('加载失败：' + e.message, '⚠️'));
      }
    }
  };
})(window);
