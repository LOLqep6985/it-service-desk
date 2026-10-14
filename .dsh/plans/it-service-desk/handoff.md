# 交接摘要：it-service-desk

> 本文件是**状态层**，描述"现在"，允许随时重写。
> 本文件是**指针，不是摘要**：只写条目 ID + 一句话 + 路径，不重复内容、不概括对话。

## 当前状态

- 计划：it-service-desk
- 阶段：Day 1-6 全部完成；PWA + 「院区→科室」两级下拉已完成；待 Day 7
- 里程碑：M0-M4 ✅；M5 签字+报销闭环 ✅，待「部署+简历」收尾
- 状态：进行中（今日收尾）
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
- 采用 project-memory v3 规范 + 新版 skill 复制到工作区 → `progress.md#S-001`

## 未完成与阻塞点

- Day 7：部署配置（Docker/Nginx）+ README 完善 + 简历项目描述 + 最终演示 → `task_plan.md#Next-Step`
- 可选体验优化：科室端列表分页、报销单 PDF 导出、基础数据管理（科室/用户增删改）

## 关键约束与决策

- 决策与踩坑全表 → `findings.md`（决策记录 + 踩坑记录）
- 关键用户原话（设备码废弃、两级下拉、localStorage 绑定）→ `findings.md#D-002`
- 环境事实（MySQL 账号、上传目录、演示账号）→ `findings.md` 与 `handoff.md` 旧版记录（已并入上表）

## 下一步

- 只做一个动作：**Day 7 部署配置**（Dockerfile + docker-compose + Nginx），完成后补 README 与简历描述 → 详见 `task_plan.md` 的 `## Next Step`

## 回读顺序（细节不够时按此顺序）

1. `findings.md` 的 D-001 / D-002 条目
2. `progress.md` 最近一节（S-001）
3. `git log --oneline -10`、`git status --short`

## 建议技能

- project-memory：自动恢复项目状态（v3 规范）
- u1-ocr-parser-pro：如需解析文档