# 交接摘要：it-service-desk

> 本文件是**状态层**，描述"现在"，允许随时重写。
> 本文件是**指针，不是摘要**：只写条目 ID + 一句话 + 路径，不重复内容、不概括对话。

## 当前状态

- 计划：it-service-desk
- 阶段：Day 1-6 全部完成；后台重构 4 批次完成；5 条已知限制已修复；闭环 100% 跑通（含财务回填）；签字体验改造完成；**权限体系 A 档落地完成（动态权限 + 财务角色 + 文案统一 + 报销进度），待用户复验**；下一步 Day 7
- 里程碑：M0-M4 ✅；**M5 签字+报销闭环 ✅ 已实跑验证**，待「部署+简历」收尾
- 状态：进行中（闭环验证完成，等用户确认后进入 Day 7）
- **交接原因**：用户切换到新会话继续开发（模型不变：DeepSeek V4.1 flash）→ 详见 `progress.md#S-011`
- **代码状态**：全部已提交，HEAD = `9e8f12f`；工作区仅剩 3 个项目记忆文档待用户提交
- **环境现状**：`MySQL80` 已 **Running**（3306 监听中）；应用由用户在 IDEA 启动（8080），AI 侧不占用该端口
- 阻塞：无
- 数据现状：**一条完整闭环演示数据**——report=1（R000001 急救中心-一层-EICU）、work_order=1（已完成/已签字/代签）、reimbursement=1（2026-10 ¥40.00 **已报销**）；sys_user 5 个、department 68 个；旧数据备份 `.rdoc_inspect/backup/demo-data-20261015-010733.sql`（详见 `progress.md#S-012`、`#S-013`）

## 不能碰的约束（必读）

- Maven 双仓库（IDEA 用 D:\Maven\repository；沙箱 CLI 用 .m2repo + 代理 7890 + JAVA_HOME=jdk17）→ `findings.md`「Maven 双仓库说明」
- Git commit/push 由**用户手动**在 VSCode 完成，AI 只提供摘要与建议提交信息 → `findings.md`「Git 工作流变更——由用户手动 commit/push」
- 科室端已去掉设备码，接口一律用 `departmentId`（保留 deviceCode 兼容）→ `findings.md#D-002`
- 项目记忆按 project-memory **v3** 规范：findings/progress 只追加、handoff 指针式 → `findings.md#D-001`

## 已完成

- 三端全流程闭环（报修→派单→维修→签字→核价→月度报销→财务回填）→ `progress.md` 各 Day 会话节
- PWA 化（manifest + service-worker + 图标，4 页接入）→ `progress.md`「体验优化——PWA 化」
- 科室端「院区→科室」两级下拉 + department 表 68 个真实科室 → `progress.md`「需求变更——去掉设备码」；提交 `1b9cafa`
- **科室端三级联动（院区→楼栋→科室）+ `department.floor` 楼层标签**（含旧库迁移脚本）→ `findings.md#D-004`、`progress.md#S-003`
- **三级联动抽为共用组件**（`js/department-cascade.js` + `css/dept-cascade.css`，report/create/admin 三页共用）、create.html 接入、管理后台「基础数据」按院区→楼栋→楼层分类 → `findings.md#D-005`、`progress.md#S-004`
- **后台重构批次 1/4**：`/api/stats/dashboard` 聚合接口（指标 + 近 7 日趋势 + 本月报销）→ `findings.md#D-006`、`progress.md#S-005`
- **后台重构批次 2/4**：前端基座（`js/admin/*` + `admin-layout.css`）+ `admin.html` 薄壳（479→45 行）+ 工作台仪表盘 + 7 个占位视图；SW 升 `itsd-v3` → `findings.md#D-007`、`progress.md#S-006`
- **后台重构批次 3/4**：7 个视图全部迁移（派单弹窗/工单详情抽屉+核价/报修历史/报销单+财务回填/科室索引/用户/系统信息）+ 新增 `/api/system/info`；SW 升 `itsd-v4` → `findings.md#D-008`、`progress.md#S-007`
- **后台重构批次 4/4 + 缺陷修复**：报销单签字留痕（`signature_url`）、维修端状态映射、防重口径、工单详情越权、密码 BCrypt；迁移脚本 `sql/2026-10-14-signature-and-bcrypt.sql`；SW 升 `itsd-v5` → `findings.md#D-009`、`progress.md#S-008`
- **数据基线重置**：清空 5 张业务表（备份可回滚）+ 空数据预检 21 项通过（2026-10-15）→ `progress.md#S-009`
- **闭环实跑验证 + 财务回填**：用户跑 ①-⑦、AI 执行第 ⑧ 步并全量核对（24 项）→ `progress.md#S-012`、`#S-013`
- **签字体验改造**：签名板重写（画布/线宽/撤销/自动裁剪）+「输入姓名生成」+ `sign_type` 留痕 + 项目首个后端单测；SW 升 `itsd-v6` → `findings.md#D-010`、`progress.md#S-014`
- **权限体系 A 档**：`WorkOrderAccess` 统一行级+状态机动态权限、详情下发 `allowedActions`、新增 FINANCE 财务角色（菜单按角色渲染）、状态文案统一、工单默认「进行中」、详情显示报销进度；明确不做单表合并/三态/流水表/冷热分离/RBAC 表/字段级脱敏框架/WebSocket → `findings.md#D-011`、`progress.md#S-015`
- 采用 project-memory v3 规范 + 新版 skill 复制到工作区 → `progress.md#S-001`

## 未完成与阻塞点

- Day 7：部署配置（Docker/Nginx）+ README 完善 + 简历项目描述 + 最终演示 → `task_plan.md#Next-Step`
- （原 `D-003` 的 5 条代码缺口**已全部修复** → `findings.md#D-009`；仍存在的 6 条限制已列在后台「系统信息」页，Day 7 写进 README）
- 可选体验优化：科室端列表分页、报销单 PDF 导出、基础数据管理（科室/用户增删改）

## 关键约束与决策

- 决策与踩坑全表 → `findings.md`（决策记录 + 踩坑记录）
- 关键用户原话（设备码废弃/两级下拉）→ `findings.md#D-002`；（三级联动三级下拉 + 楼层标签）→ `#D-004`；（抽共用组件 + 基础数据分类）→ `#D-005`；（后台重构方案与四项选择）→ `#D-006`；（5 条限制「修」）→ `#D-009`
- 环境事实（MySQL 账号、上传目录、演示账号、8090/8080 约定）→ `progress.md#S-010` 与 `findings.md`

## 下一步

- 用户复验（SW 已升 `itsd-v7`，首次打开请 Ctrl+Shift+R）：
  1. **财务账号** `finance / 123456` 登录 → 应直接进「月度报销单」，侧栏只有「财务与报销」一项；用 admin 对比菜单差异
  2. 后台「工单管理」默认只显示进行中，下拉切「全部状态」可看已完工
  3. 打开任一工单详情，确认「报销进度」行与「保存核价」按钮
  4. 科室端签字流程（手写 / 输入姓名生成）
- 数据现状：report=1 / work_order=1（COMPLETED·SIGNED，sign_type=NULL→显示「未记录」）/ consumable=1 / reimbursement=1（REIMBURSED ¥40.00）；账号 6 个（含 finance）、科室 68 个；备份 `.rdoc_inspect/backup/demo-data-20261015-010733.sql`
- 之后 **Day 7**：部署配置（**需先确认目标：本机 Docker Desktop 演示 or 内网 Linux + HTTPS**）+ README 重写（6 个演示账号 + BCrypt + 迁移脚本 + PWA 需 HTTPS）+ 简历项目描述 + 演示截图
- 后续版本可选（均已在 `findings.md#D-011` 记录理由）：已挂起/已驳回/已取消 + 状态流水表（一起做）；SSE 实时推送；签字「上传照片」/「二维码转手机签字」；服务端分页
- 常规回归套件（需先起 8090 实例）：`mvn test`（17 项）→ `test-signature-pad.js`（22）→ `test-frontend.js` → `test-admin-app.js` → `test-finance-api.js` → `test-sign-type.js` → `test-batch4-api.js`（会造数据，跑完按 id>1 清理）

## 回读顺序（细节不够时按此顺序）

1. `findings.md` 的 D-001 ~ D-011（D-006 方案/批次，D-007 前端架构，D-009 五项修复，D-010 签字改造，D-011 权限体系与"不做什么"清单）
2. `progress.md` 最近三节（S-013 闭环验证、S-014 签字改造、S-015 权限 A 档）
3. `git log --oneline -10`、`git status --short`

## 建议技能

- project-memory：自动恢复项目状态（v3 规范）
- u1-ocr-parser-pro：如需解析文档
