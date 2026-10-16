# 交接摘要：it-service-desk

> 本文件是**状态层**，描述"现在"，允许随时重写。
> 本文件是**指针，不是摘要**：只写条目 ID + 一句话 + 路径，不重复内容、不概括对话。

## 当前状态

- 计划：it-service-desk（`.active_plan` 指向本目录）
- 阶段：Day 1-6 完成；**2026-10-16 完成 8 轮大改（S-025~S-038）**；**Day 7（部署/README/简历）未开始**
- 里程碑：M0-M4 ✅；M5 进行中（签字+报销闭环已实跑验证，差部署/README/简历）
- 代码状态：HEAD=`b92fb40`（已提交到 S-034）；**S-035~S-038 大量改动未提交**（见下方清单）
- 环境：MySQL80 Running(3306)；无 java 进程；8080 空闲（留给用户在 IDEA 启动）；8099 未占用
- 数据：**已清库恢复初始状态**（2026-10-16 21:22）；report/work_order/consumable/reimbursement/reimbursement_item/department_binding 均 0 行；sys_user 6、department 68；uploads 0 文件

## 本轮未提交改动（S-035~S-038，按子系统归类）

→ `findings.md#D-021`~`#D-027`、`progress.md#S-025`~`#S-038`

### 报销单分池系统（D-026/D-027，核心改动）
| 文件 | 一句话改动 |
|---|---|
| `ReimbursementStatus.java` | 加 VOIDED + RETURNED 状态 |
| `ReimbursementService.java` | poolSummary() + generateFromPool() + revoke/returnByNurse/returnByFinance + 占用校验调整 + calcOrdersTotal |
| `ReimbursementController.java` | /pool /generate-from-pool /revoke /return-finance /return-nurse 五接口 |
| `static/js/admin/views/reimbursements.js` | 重写为科室池卡片网格 + 抽屉勾选生成 + 撤回/退回按钮 |
| `static/js/admin/ui.js` | STATUS.REIMBURSEMENT 加 VOIDED/RETURNED |

### 工单流程优化
| 文件 | 一句话改动 |
|---|---|
| `WorkOrderService.java` | submitRepair 无耗材直接 PENDING_SIGN 跳过核价 |
| `static/js/admin/views/work-orders.js` | 下拉默认全部状态+删进行中+删报修单项+删签字列 |
| `static/today.html` | 接单后 500ms 自动跳详情 |

### 科室端
| 文件 | 一句话改动 |
|---|---|
| `static/report.html` | 抬头居中自适应 + 10秒防抖(含失败冷却) + 报修记录反查报销单 + 签字弹窗加退回按钮 + 修 reimbJson 自引用崩溃 |

### 后台菜单与登录
| 文件 | 一句话改动 |
|---|---|
| `static/js/admin/layout.js` | 菜单恢复「待受理报修单+工单管理」两项 |
| `static/js/admin/views/dashboard.js` | 「去派单」跳回 /reports/pending |
| `static/admin.html` | 删 reports-history 引用，恢复 reports-pending 引用 |
| `static/js/admin/views/reports-pending.js` | 重建（上轮误删后恢复） |
| `static/js/admin/app.js` | storage 事件身份看守 |
| `static/login.html` | 静默覆盖旧登录态（撤掉 confirm） |

### 记忆文件（4 个）
| 文件 | 一句话改动 |
|---|---|
| `findings.md` | 追加 D-024~D-027 |
| `progress.md` | 追加 S-037、S-038 |
| `task_plan.md` | 待更新（下一会话） |
| `handoff.md` | 本文件重写 |

建议分两次提交（报销单系统是独立大改）：
1. `feat(报销): 按科室分池+勾选生成+撤回退回+报修端同步（D-026/D-027）`
2. `feat(工单/科室端): 无耗材跳核价+报修防抖+接单跳转+删除冗余签字列+身份看守+修报修记录崩溃`

## Next Step（从这里开始）

1. **用户继续全流程验证**（上次中途报修记录崩溃已修，工单签字列已删，重启后续走）：
   - 报修→派单→接单（自动跳详情）→无耗材直接待签字 / 有耗材待核价→核价→签字→待报销池→勾选生成报销单→护士长签字→财务回填
2. **验证通过后二选一**：
   - 接企微群机器人（代码已就绪 D-026，缺 webhook；用户单微信号无法同时验管理员+维修工，暂缓）
   - 进 Day 7：部署配置（云服务器/内网穿透）+ README + 简历项目描述 → `task_plan.md#Next-Step`
3. 长期残留：跨端实时推送（D-012）未做

## 不能碰的约束（必读）

- Maven 双仓库：IDEA 用 `D:\Maven\repository`；沙箱 CLI 用 `-Dmaven.repo.local=.m2repo`，且**必须** `JAVA_HOME=D:\DevTools\Java\jdk17`（默认 JAVA_HOME 是 jdk8，报"无效目标发行版 17"）→ `findings.md`
- **`mvn package` 前必须先停掉占用 `target/desk-0.1.0.jar` 的 java 进程**，否则报 `Unable to rename ... .jar.original`
- Git commit/push 由**用户手动**完成，AI 只给摘要与建议提交信息 → `findings.md`
- 科室端接口一律用 `departmentId`（`deviceCode` 仅兼容保留）→ `findings.md#D-002`
- 记忆规范：`findings.md`/`progress.md` 只追加，`handoff.md`/`task_plan.md` 可重写 → `findings.md#D-001`
- 改动前端后无需 bump SW 版本（已是网络优先）；首次仍建议 Ctrl+Shift+R → `findings.md#D-021`
- **D-024 报修单/工单清单归属**：后台只管工单；报修单只在「待受理报修单」页展示，不进工单管理下拉
- 报销单已作废/已退回后，工单回到待报销池可重新勾选（占用校验只算活跃报销单）
- 设备照片功能已彻底移除，不要再加回 → `progress.md#S-031`

## 数据与环境事实

- 演示账号（BCrypt，见 `data.sql`）：`admin/admin123`、`dispatcher/123456`、`repairer1/123456`、`repairer2/123456`、`reporter1/123456`、`finance/123456`
- 备份目录 `.rdoc_inspect/backup/`：business-data-20261016-212234.sql（清库前）、orphan-uploads-20261016-212234/（16 图）
- 企微通知配置位：`application.yml` 的 `notify.wecom.webhook` + `notify.public-host`（**当前为空=功能关闭**）→ `progress.md#S-026`
- 用户机器内网地址：`10.31.67.107`（二维码/通知链接需用它）
- 用户单微信号：无法同时验证管理员派单+维修工收通知，企微接通暂缓

## 本地验证工具（`.rdoc_inspect/` 已被 gitignore，仅本机可用）

| 脚本 | 覆盖 | 前置 |
|---|---|---|
| verify-back-key.js | 返回键防重入（11 项） | 8080 运行 |
| verify-roles-ui.js | 角色按钮可见性（11 项） | 8080 运行 |
| verify-reimburse-ui.js | 财务回填 + 跨端同步（15 项） | 8080 运行 |
| verify-ui.js | 科室端/登录页（19 项） | 需先起 temp-static-server.js(8099) |
| temp-static-server.js | 8099 临时静态服务 | 验完必须关闭 |

## 回读顺序（细节不够时按此顺序）

1. `findings.md` 的 **D-027（撤回退回）、D-026（分池）、D-024（清单归属）、D-025（身份看守）**
2. `progress.md` 最近 3 节（S-038、S-037、S-036）
3. `git log --oneline -8`（HEAD=`b92fb40`；S-035~S-038 待提交）

## 建议技能

- project-memory：恢复/落盘项目状态
- design-taste-frontend：前端视觉改造时（历史用于 D-020 去 slop）