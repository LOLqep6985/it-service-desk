# 交接摘要：it-service-desk

> 本文件是**状态层**，描述"现在"，允许随时重写。
> 本文件是**指针，不是摘要**：只写条目 ID + 一句话 + 路径，不重复内容、不概括对话。

## 当前状态

- 计划：it-service-desk（`.active_plan` 指向本目录）
- 阶段：Day 1-6 完成；**Day 7「部署」2026-10-17 打通（内网单机）**；**同日按用户意见移除企微业务 + 取消 PWA 安装引导（S-040/D-029）**；Day 7 剩余 README/简历
- 里程碑：M0-M4 ✅；M5 进行中（签字+报销闭环已验证；部署待防火墙+手机实测；简历未写）
- 代码状态：HEAD=`3fc4d73`；**未提交**：`deploy/`（新增脚本+手册+qr）、`.gitignore`、删 2 文件（`WecomWebhookNotifier.java`/`pwa-install.js`）、改 9 文件、4 个记忆文件
- 运行态：**8080/8081 均已停止**（交还给用户；用户会用 IDEA 起 8080）
- 数据：**2026-10-17 已迁移到干净基线**——业务表全 0，sys_user 6、department 68；`uploads/` 0 文件
- 环境：MySQL80 Running(3306)；`10.31.67.107`(WLAN, /16)；无 Docker；非管理员（防火墙/进程管理受限）

## 本轮改动（2026-10-17，两批）

### 批次 A：部署（S-039 / D-028）

| 文件 | 一句话 |
|---|---|
| `deploy/start-desk.ps1` | 一键启动：端口检查→内网 IP 探测→JDK17 实测校验→启动→轮询 200 才算 READY |
| `deploy/stop-desk.ps1` | 先按 jar 命令行、再按 `-Port` 用 `netstat` 兜底；非 java 一律 SKIP |
| `deploy/README.md` | 部署手册（含第九节「明确不做的部分」） |
| `deploy/qr/{report,today,login}.png` | 三端入口二维码（240×240） |
| `.gitignore` | 加 `deploy/logs/` |

### 批次 B：精简（S-040 / D-029，需求推翻）

| 文件 | 一句话 |
|---|---|
| ~~`service/WecomWebhookNotifier.java`~~ | **已删除**（企微群机器人通知） |
| ~~`static/js/pwa-install.js`~~ | **已删除**（安装引导气泡） |
| `service/ReportService.java` | 去 notifier/CAMPUS_LABEL/userMapper；构造器 5 参→3 参 |
| `controller/SystemInfoController.java` | 去 `notify-test` 接口与 `data.notify` |
| `resources/application.yml` | 删整个 `notify:` 块 |
| `js/admin/views/system.js` | 删 `notifyRows()` 与「派单通知」行；「PWA 安装条件」→「桌面图标」 |
| `static/service-worker.js` | 去 `pwa-install.js` 与死链 `reports-history.js`；`itsd-v20`→`itsd-v21` |
| `static/{report,today,detail,login,create}.html` | 各删 1 行 `pwa-install.js` 引用 |

## Next Step（从这里开始）

> 唯一主线：把"手机能打开"收口，其余后置

1. 防火墙放行（**管理员**）：`netsh advfirewall firewall add rule name="ITSD-8080" dir=in action=allow protocol=TCP localport=8080` → `deploy/README.md` 第五节
2. 端口归一：停 IDEA 的 8080 实例 → `deploy\start-desk.ps1 -Port 8080` → 重出二维码 → 扫 `deploy/qr/report.png`
3. 手机实测 `http://10.31.67.107:8080/report.html`（**唯一有效验收**）
4. 目视确认「系统信息」页已无企微块 → `progress.md#S-040`
5. 开机自启/服务化 → `deploy/README.md` 第六节
6. git 提交（建议分两次：部署 / 精简）
7. **待商量**：手机端弹出消息，不设计 → `deploy/README.md` 第九节

## 不能碰的约束（必读）

- **不做企业微信**：企微/群机器人/webhook 相关代码、配置、接口、页面提示已整体移除，**不要再加回** → `findings.md#D-029`（supersedes `D-021` 的 PWA 引导部分与 `S-026`）
- **不做 PWA 安装引导**：桌面图标由使用人员自行用浏览器菜单「添加到主屏幕」；**保留 manifest**（桌面图标要用）与 **service-worker**（缓存策略，与安装无关）→ `D-029`
- **手机端弹出消息**：暂不实现，只标注待商量，不展开设计 → `D-029`
- **科室报修端扫码入口必须保留**：扫码即用，不做账号体系 → `D-029`
- **.ps1 必须 UTF-8 with BOM**：PS 5.1 按 GBK 读，中文注释会截断字符串；`edit` 工具会丢 BOM，改完要补 → `D-028`
- **JAVA_HOME 默认 jdk8**：必须实测 `java -version` ≥17；`java -version` 走 stderr，函数内要临时 `ErrorActionPreference=Continue` → `D-028`
- **mvn 参数要加引号**：`"-Dmaven.repo.local=.m2repo"` → `progress.md#S-039`
- **沙箱内 CIM 不可用**（`Get-NetTCPConnection`/`tasklist`/`Get-CimInstance` 静默失败）→ 端口检查用 `netstat -ano` → `D-028`
- **内网 IP 探测要排除虚拟网卡**（WSL/Hyper-V/VMware），否则拿到 `172.23.240.1` → `D-028`
- **同机访问内网 IP 成功 ≠ 手机能访问**（不经入站防火墙）→ `deploy/README.md`
- **改 `APP_SHELL` 必须 bump 缓存版本**（普通页面改动不用）；http 下 SW 本就不生效 → `progress.md#S-040`
- 取真实行数用 `COUNT(*)`，`information_schema.table_rows` 是估算 → `D-028`
- Git commit/push 由**用户手动**完成；科室端接口用 `departmentId`；记忆规范 findings/progress 只追加 → `findings.md#D-001`/`#D-002`
- 报销单已作废/退回后工单回池可重勾；设备照片功能已移除勿加回 → `progress.md#S-031`

## 数据与环境事实

- 演示账号（BCrypt，见 `data.sql`）：`admin/admin123`、`dispatcher/123456`、`repairer1|repairer2/123456`、`reporter1/123456`、`finance/123456`
- 迁移前备份：`.rdoc_inspect/backup/pre-deploy-20261017-143714.sql`（19,227 B，`Dump completed` 已校验）
- 签名图归档：`.rdoc_inspect/backup/uploads-pre-deploy-20261017-143731/`（8 张，108 KB）
- 日志：`deploy/logs/desk-<ts>.out.log`（已 gitignore）
- 运行期环境变量只剩三类：`SERVER_PORT`、`FILE_UPLOAD_DIR`、`DB_URL/DB_USERNAME/DB_PASSWORD`

## 回读顺序（细节不够时按此顺序）

1. `findings.md#D-029`（本次需求推翻：删除清单 + 保留边界）
2. `progress.md#S-040`（改动 11 文件 + 验证证据）
3. `findings.md#D-028` + `progress.md#S-039`（部署决策与环境坑）
4. `deploy/README.md`（可执行步骤全在这）
5. `git log --oneline -6`（HEAD=`3fc4d73`）

## 建议技能

- project-memory：恢复/落盘项目状态
- diagnose-windows-sandbox-acl：沙箱写/读异常时排查 ACL

## 新增交接（2026-10-17 下午：消息通知 A 档）

- 决策：`findings.md#D-030`（4 事件 + 轮询 + 无回执 + 无免打扰 + A 档）
- 实现与实测证据：`progress.md#S-041`
- 新增文件：`util/LanAddress.java`、`model/Notification.java`、`model/NotificationType.java`、`mapper/NotificationMapper.java`、`service/NotificationService.java`、`controller/NotificationController.java`、`service/UnacceptedTimeoutJob.java`、`static/js/notifications.js`、`sql/2026-10-17-notification.sql`
- 改动：`schema.sql`、`application.yml`（无）、`QrController`、`SystemInfoController`、`ReportController`、`ReportService`、`WorkOrderService`、`DepartmentBindingController`、`Report`/`DepartmentBinding`/`AnonymousReportRequest`、`system.js`、`report.html`/`today.html`/`admin.html`、`service-worker.js`(v22)
- **上线前必做**：跑一次 `sql/2026-10-17-notification.sql`（本机已跑，其他环境未跑）
- **待用户确认**：真机上声音/震动/角标是否正常（首击解锁音频）；科室负责人姓名是否要批量补