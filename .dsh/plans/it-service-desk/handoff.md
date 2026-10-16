# 交接摘要：it-service-desk

> 本文件是**状态层**，描述"现在"，允许随时重写。
> 本文件是**指针，不是摘要**：只写条目 ID + 一句话 + 路径，不重复内容、不概括对话。

## 当前状态

- 计划：it-service-desk（`.active_plan` 指向本目录）
- 阶段：Day 1-6 完成；**2026-10-16 完成 6 轮前端/权限/安全改动（S-025~S-035）**；**Day 7（部署/README/简历）未开始**
- 里程碑：M0-M4 ✅；M5 进行中（签字+报销闭环已实跑验证，差部署/README/简历）→ `task_plan.md#里程碑`
- 代码状态：HEAD = `b92fb40`（一次补提交，含 S-025~S-034 全部改动）；**本轮 S-035 共 9 个文件未提交（5 代码 + 4 记忆）**
- 环境：MySQL80 Running(3306)；**无 java 进程**；8080 空闲（留给用户在 IDEA 启动）；8099 未占用
- 数据：report#1/#3、work_order#1/#3、reimbursement#1/#3 均 `REIMBURSED`；`sys_user` 6、`department` 68；`uploads/` 4 个文件**全部被 DB 引用，无孤儿**

## 本轮未提交改动（S-035，共 9 文件 = 5 代码 + 4 记忆）

→ `progress.md#S-035`、`#S-036`、`findings.md#D-023`

| 文件 | 一句话改动 |
|---|---|
| `src/main/java/com/itservice/desk/config/WebConfig.java` | HTML/`/` 下发 `Cache-Control: no-store` |
| `src/main/resources/static/js/guard.js` | 退出改 `location.replace` + `pageshow(persisted)` 重校验 |
| `src/main/resources/static/js/admin/app.js` | 未登录静默跳转（不再弹窗）+ bfcache 重校验 |
| `src/main/resources/static/js/admin/store.js` | `logout()` 改 `location.replace` |
| `src/main/resources/static/report.html` | 科室端「退出」改 `location.replace` |
| `.dsh/plans/it-service-desk/findings.md` | 追加 `#D-023` |
| `.dsh/plans/it-service-desk/progress.md` | 追加 `#S-035`、`#S-036` |
| `.dsh/plans/it-service-desk/task_plan.md` | 状态层重写（当前阶段/Next Step/已完成/待办） |
| `.dsh/plans/it-service-desk/handoff.md` | 状态层重写（本文件） |

建议提交信息（代码 + 记忆一起提交）：
`fix(安全): 退出登录改用 replace + bfcache 重校验 + HTML no-store，防止返回键重入已登录界面`

## 2026-10-16 六轮改动（全部已提交，除 S-035）

| 轮次 | 一句话 | 指针 |
|---|---|---|
| S-025 | PWA 安装引导 + 三端入口二维码 + 端间角色隔离与统一退出 | `findings.md#D-021`、`progress.md#S-025`，提交 `89d2e6a` |
| S-026 | 企业微信群机器人派单通知（配置驱动，默认关闭） | `progress.md#S-026`，提交 `a642d1e` |
| S-027 | 指标卡行自适应拉伸 + 统一间距与卡面边线 | `progress.md#S-027`，提交 `1e826bd` |
| S-028 | 科室端退出按钮（管理端同款）+ 登录页回车登录 | `progress.md#S-028`，提交 `8d82857` |
| S-029 | 修 `ui.confirm` Promise 竞态（财务回填静默失效） | `progress.md#S-029`，提交 `b92fb40` |
| S-030 | 清库恢复初始状态（含备份） | `progress.md#S-030`，提交 `b92fb40` |
| S-031 | **彻底移除设备照片功能**（前后端+数据库） | `progress.md#S-031`、`sql/2026-10-16-drop-device-photo.sql` |
| S-032 | 清理 uploads 孤儿文件（移入备份） | `progress.md#S-032` |
| S-033 | **SW 静态资源改网络优先**（根治"改了不生效"） | `progress.md#S-033`、`findings.md#D-021` |
| S-034 | **报销职责分离**：生成归运维、回填归财务 | `findings.md#D-022`、`progress.md#S-034`，提交 `b92fb40` |
| S-035 | 浏览器返回键防重入（三层防护） | `findings.md#D-023`、`progress.md#S-035`，**未提交** |

## Next Step（从这里开始）

1. **用户复验 S-035**：IDEA 启动 8080 → 强刷 → 登录后台 → 点右上角退出 → 按返回键（Alt+←）→ 应仍在登录页
2. **git 提交本轮 9 文件**（5 代码 + 4 记忆，建议信息见上）
3. **（未开工，二选一）**
   - 配置企微 webhook 并点「系统信息 → 发送测试通知」验证派单通知 → `progress.md#S-026`
   - 或进入 **Day 7**：部署配置（Docker/Nginx）+ README + 简历项目描述 → `task_plan.md#Next-Step`
4. 长期残留：**D-012 跨端实时推送**未做 → `findings.md#D-012`

## 不能碰的约束（必读）

- Maven 双仓库：IDEA 用 `D:\Maven\repository`；沙箱 CLI 用 `-Dmaven.repo.local=.m2repo`，且**必须** `JAVA_HOME=D:\DevTools\Java\jdk17`（默认 JAVA_HOME 是 jdk8，报"无效目标发行版 17"）→ `findings.md`「Maven 双仓库说明」
- **`mvn package` 前必须先停掉占用 `target/desk-0.1.0.jar` 的 java 进程**，否则报 `Unable to rename ... .jar.original`
- Git commit/push 由**用户手动**完成，AI 只给摘要与建议提交信息 → `findings.md`「Git 工作流变更」
- 科室端接口一律用 `departmentId`（`deviceCode` 仅兼容保留）→ `findings.md#D-002`
- 记忆规范：`findings.md`/`progress.md` 只追加，`handoff.md`/`task_plan.md` 可重写 → `findings.md#D-001`
- **改动前端后无需再 bump SW 版本**（已是网络优先）；但首次仍建议 Ctrl+Shift+R → `findings.md#D-021`、`progress.md#S-033`
- 权限矩阵：报销**生成**=管理员/调度员，**回填**=仅财务；工单级报销入口已移除 → `findings.md#D-022`
- 设备照片功能已**彻底移除**（含 `report.device_photo` 列），不要再加回 → `progress.md#S-031`

## 数据与环境事实

- 演示账号（BCrypt，见 `data.sql`）：`admin/admin123`、`dispatcher/123456`、`repairer1/123456`、`repairer2/123456`、`reporter1/123456`、`finance/123456`
- 备份目录 `.rdoc_inspect/backup/`：`business-data-20261016-150758.sql`（清库前全量）、`business-data-20261015-214245.sql`、`demo-data-20261015-010733.sql`、`orphan-uploads-20261016-153443/`（16 个孤儿图，可整目录删）
- 企微通知配置位：`application.yml` 的 `notify.wecom.webhook` + `notify.public-host`（**当前为空=功能关闭**）→ `progress.md#S-026`
- 用户机器内网地址：`10.31.67.107`（二维码/通知链接需用它，不能用 localhost）

## 本地验证工具（`.rdoc_inspect/` 已被 gitignore，仅本机可用）

| 脚本 | 覆盖 | 前置 |
|---|---|---|
| `verify-back-key.js` | 返回键防重入（11 项） | 8080 运行 |
| `verify-roles-ui.js` | 角色按钮可见性（11 项） | 8080 运行；无「待报销」数据时相关项自动 SKIP |
| `verify-reimburse-ui.js` | 财务回填 + 跨端同步（15 项） | 8080 运行；需存在「待报销」单，可用 `TARGET_REIMB_ID=<id>` 指定目标 |
| `verify-ui.js` | 科室端/登录页（19 项） | 需先起 `temp-static-server.js`(8099) |
| `temp-static-server.js` | 8099 临时静态服务 | 验完必须关闭 |
| `patch_*.mjs` / `test-*.js` | 历史批次脚本（旧） | 参考即可 |

## 回读顺序（细节不够时按此顺序）

1. `findings.md` 的 **D-023（返回键）、D-022（职责分离）、D-021（PWA/SW/三端）**
2. `progress.md` 最近 3 节（S-035、S-034、S-033）
3. `git log --oneline -8`（HEAD=`b92fb40`；S-035 待提交）

## 建议技能

- project-memory：恢复/落盘项目状态
- design-taste-frontend：前端视觉改造时（历史用于 D-020 去 slop）
