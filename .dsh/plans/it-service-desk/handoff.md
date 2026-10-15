# 交接摘要：it-service-desk

> 本文件是**状态层**，描述"现在"，允许随时重写。
> 本文件是**指针，不是摘要**：只写条目 ID + 一句话 + 路径，不重复内容、不概括对话。

## 当前状态

- 计划：it-service-desk
- 阶段：Day 1-6 完成；后台重构 4 批次完成；权限 A 档完成；**工单状态机重构（D-015）严格对齐《工单全流程规则.md》6 态** → `findings.md#D-013`/`#D-014`/`#D-015`、`progress.md#S-017`/`#S-018`/`#S-019`
- 里程碑：M0-M4 ✅；M5 闭环 ✅；**工单状态机终态闭环 = 待接单→维修中→待核价→待签字→待报销→已报销；报修单 = 待受理→已派单→已完成→已报销**
- 状态：**本次改动已完成并自测全绿，待用户复验与提交**
- 代码状态：HEAD 仍为 `472c30e`（A 档）；本次改 7 个文件 + 新增 1 个 sql + 记忆文档，**均未提交**（git commit/push 由用户手动）
- 环境现状：MySQL80 Running（3306）；8080 留给 IDEA；8090 临时实例已停，无 java 进程
- 数据现状：report 2（R000001/R000013 均 COMPLETED）、work_order 2（#1/#7 均 COMPLETED+SIGNED）、consumable 1、reimbursement 1（2026-10 ¥40 已报销）；sys_user 6 个、department 68 个

## 本次改动（2026-10-15，三轮）

- **D-013 工单业务管理重构**（报修单状态同步+历史整合）→ `findings.md#D-013`、`progress.md#S-017`
- **D-014 报销闭环同步**（财务回填→已报销）→ `findings.md#D-014`、`progress.md#S-018`
- **D-015 工单状态机重构**（严格对齐《工单全流程规则.md》6态）→ `findings.md#D-015`、`progress.md#S-019`
- **D-016 科室索引修复**（搜索失焦根因=重建整卡；竖列根因=.dept-* CSS 类从未定义；院区三栏 grid 重排）→ `findings.md#D-016`、`progress.md#S-020`；**附带发现 department_head 全空**
- **D-017 科室索引搜索移除**（用户裁定冗余，只留刷新按钮）→ `findings.md#D-017`、`progress.md#S-021`
- **D-018 签字弹窗费用明细**（核价后费用对签字人透明：耗材×数量×单价+合计，未核价标红）→ `findings.md#D-018`、`progress.md#S-022`；**数据库已清空配合用户全流程验证**（备份 business-data-20261015-214245.sql）；⚠️ 曾误杀用户 8080 实例（教训：杀进程必须按端口定位）
- **D-019 UI文案+详情费用+数据留存**（按《数据留存策略.md》：报修记录标题/详情弹窗费用/报修60天+报销180天查询过滤，不删数据）→ `findings.md#D-019`、`progress.md#S-023`
- **D-020 前端去 slop 重构**（design-taste-frontend skill：44 FAIL→0，删阴影/细线/描边/渐变/emoji，对比度修复，字号拉大，三端同等力度）→ `findings.md#D-020`、`progress.md#S-024`；SW itsd-v15
- 落地文件：`WorkOrderStatus.java`(6态去PENDING/COMPLETED加PENDING_PRICING/PENDING_REIMBURSE/REIMBURSED)、`WorkOrderService.java`(+completePricing/reimburse, sign→待报销, submitRepair→待核价)、`WorkOrderAccess.java`(+REIMBURSE)、`WorkOrderController.java`(pricing语义变更+/{id}/reimburse)、`ReimbursementService.java`(reimburse回填工单→已报销+注入WorkOrderService)、`StatsService`+`DashboardStats`+`SystemInfoController`、前端 ui.js/work-orders.js/system.js/dashboard.js/app.js/today/detail/report/reports-history、`sql/2026-10-15-workorder-status-machine.sql`+2个前置迁移、`WorkOrderAccessTest.java`(12项)、SW itsd-v10
- 验证：三轮闭环实跑全绿（签字→待报销→已报销；7步状态机每步查库正确）+ mvn test + 前端四套回归
- 工单状态机（严格6态）：待接单→维修中→待核价→待签字→待报销(移入历史)→已报销
- 报修单状态机（粗粒度）：待受理→已派单→已完成→已报销

## 不能碰的约束（必读）

- Maven 双仓库（IDEA 用 D:\Maven\repository；沙箱 CLI 用 .m2repo + 代理 7890 + JAVA_HOME=jdk17）→ `findings.md`「Maven 双仓库说明」
- Git commit/push 由**用户手动**在 VSCode 完成，AI 只提供摘要与建议提交信息 → `findings.md`「Git 工作流变更」
- 科室端已去掉设备码，接口一律用 `departmentId`（保留 deviceCode 兼容）→ `findings.md#D-002`
- 项目记忆按 project-memory **v3** 规范：findings/progress 只追加、handoff 指针式 → `findings.md#D-001`
- mvn test 在沙箱需用 `-Dmaven.repo.local=.m2repo`（surefire-junit-platform 需在线拉到项目内仓库；沙箱无权限写 D:\Maven\repository）

## 已完成

- 三端全流程闭环（报修→派单→维修→签字→核价→月度报销→财务回填）→ `progress.md` 各 Day 会话节
- PWA 化 + 三级联动组件化 + 后台信息架构重构 4 批次 + 5 条已知限制修复 + 签字体验改造 + 权限 A 档 → `findings.md#D-004`~`#D-011`
- **本次：报修单状态随工单流转同步回写 + 科室端历史列表整合** → `findings.md#D-013`、`progress.md#S-017`

## 未完成与阻塞点

- **D-012 跨端实时推送（残留）**：用户本次诉求是工单业务管理的状态同步与历史展示，已解决；但「派单后维修端自动看到/签字后后台自动更新」这类实时性仍未做 → `findings.md#D-012`
- Day 7：部署配置（Docker/Nginx）+ README 完善 + 简历项目描述 + 最终演示 → `task_plan.md#Next-Step`
- 可选体验优化：科室端列表分页、报销单 PDF 导出、基础数据管理（科室/用户增删改）

## 关键约束与决策

- 决策与踩坑全表 → `findings.md`
- 关键用户原话（设备码废弃/两级下拉）→ `#D-002`；（三级联动+楼层）→ `#D-004`；（后台重构方案）→ `#D-006`；（5 条限制修复）→ `#D-009`；（签字改造）→ `#D-010`；（权限 A 档不做什么）→ `#D-011`；（状态同步未决）→ `#D-012`；（工单业务重构）→ `#D-013`；（报销闭环同步）→ `#D-014`；（工单状态机6态重构）→ `#D-015`
- 环境事实（MySQL 账号、上传目录、演示账号、8090/8080 约定）→ `progress.md#S-010` 与 `findings.md`
- 演示账号密码：admin/admin123、dispatcher/123456、repairer1/123456、repairer2/123456、reporter1/123456、finance/123456（BCrypt 哈希，见 data.sql）

## 下一步

- **第 0 步（用户复验）**：用户正在全流程验证（R000001 已完成/报销单#1 待报销）——**需先重启 IDEA 的 8080**（此前被误杀）；科室端：报修记录标题改为「报修记录（仅展示近 2 个月）」、详情弹窗新增「报修费用」区（耗材×数量×单价+合计）、签字弹窗费用明细（D-018）；科室索引无搜索框+三栏布局；工单管理 4 态+报销按钮；系统信息 6 态状态机图。SW 已升 itsd-v14，首次打开 Ctrl+Shift+R
- 复验通过后：**git 提交**（三条建议信息见 `progress.md` 的 Git 提交记录节，可合并或分开）
- 之后 **D-012 跨端实时推送**（焦点自动刷新 ~10 行 / SSE 单向推送约 1 批次，二选一）或 **Day 7** 部署/README/简历
- 常规回归（需先起 8090 实例）：`mvn -Dmaven.repo.local=.m2repo test`（17 项）→ `test-frontend.js` → `test-admin-app.js` → `test-sign-type.js` → `test-finance-api.js`（造数据后按 id 清理）

## 回读顺序（细节不够时按此顺序）

1. `findings.md` 的 **D-013（本次工单业务重构）、D-012（跨端推送残留）、D-011（权限不做什么）**
2. `progress.md` 最近两节（S-017 本次、S-016 用户复验反馈）
3. `git log --oneline -10`（A 档 HEAD=`472c30e`；本次改动待提交）

## 建议技能

- project-memory：自动恢复项目状态（v3 规范）
- u1-ocr-parser-pro：如需解析文档
