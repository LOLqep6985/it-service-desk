# 交接摘要：it-service-desk

> 本文件是**状态层**，描述"现在"，允许随时重写。
> 本文件是**指针，不是摘要**：只写条目 ID + 一句话 + 路径，不重复内容、不概括对话。

## 当前状态

- 计划：it-service-desk
- 阶段：Day 1-6 全部完成；PWA + 三级联动组件化 + 管理后台基础数据分类已完成；**管理后台信息架构重构进行中（批次 1/4 完成，等用户验证）**；Day 7 未开始
- 里程碑：M0-M4 ✅；M5 签字+报销闭环 ✅，待「部署+简历」收尾
- 状态：进行中（后台重构批次 1 已交付，等验证后做批次 2）
- 阻塞：无

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
- 采用 project-memory v3 规范 + 新版 skill 复制到工作区 → `progress.md#S-001`

## 未完成与阻塞点

- Day 7：部署配置（Docker/Nginx）+ README 完善 + 简历项目描述 + 最终演示 → `task_plan.md#Next-Step`
- 已知代码缺口（可选修复，非阻塞）→ `findings.md#D-003`
- 可选体验优化：科室端列表分页、报销单 PDF 导出、基础数据管理（科室/用户增删改）

## 关键约束与决策

- 决策与踩坑全表 → `findings.md`（决策记录 + 踩坑记录）
- 关键用户原话（设备码废弃、两级下拉、localStorage 绑定）→ `findings.md#D-002`
- 环境事实（MySQL 账号、上传目录、演示账号）→ `findings.md` 与 `handoff.md` 旧版记录（已并入上表）

## 下一步

- 等用户验证批次 1（`/api/stats/dashboard`）→ 进入**批次 2**：前端基座（`admin-layout.css` + `js/admin/{api,store,router,layout,ui,app}.js`）+ `admin.html` 薄壳 + 空视图占位
- 批次 3：8 个视图迁移；批次 4：SW 升 v3 + 全量回归 + 记忆落盘 → 详见 `findings.md#D-006` 的批次计划
- 后台重构完成后：Day 7 部署（Docker/Nginx）+ README（含去掉陈旧的「设备码 DEV001」）+ 简历描述

## 回读顺序（细节不够时按此顺序）

1. `findings.md` 的 D-001 ~ D-006 条目（D-006 = 后台重构方案与批次计划）
2. `progress.md` 最近一节（S-005）
3. `git log --oneline -10`、`git status --short`

## 建议技能

- project-memory：自动恢复项目状态（v3 规范）
- u1-ocr-parser-pro：如需解析文档