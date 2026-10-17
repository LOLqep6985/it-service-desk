# 交接摘要：it-service-desk

> 本文件是**状态层**，描述"现在"，允许随时重写。
> 本文件是**指针，不是摘要**：只写条目 ID + 一句话 + 路径，不重复内容、不概括对话。

## 当前状态

- 计划：it-service-desk（`.active_plan` 指向本目录）
- 阶段：Day 1-6 完成；**Day 7 的「部署」部分 2026-10-17 打通（内网单机）**；Day 7 剩余 README/简历
- 里程碑：M0-M4 ✅；M5 进行中（签字+报销闭环已验证；部署待防火墙+手机实测；简历未写）
- 代码状态：HEAD=`3fc4d73`；**本轮新增 `deploy/`（脚本+手册+qr）、`.gitignore` 加 `deploy/logs/`、4 个记忆文件，均未提交**
- 运行态：**8080 = 用户 14:06 在 IDEA 启动的开发实例（`publicHost` 为空）**；**8081 = 本轮受控部署实例（PID 14636，`publicHost=http://10.31.67.107:8081`）**，两者并存
- 数据：**2026-10-17 已迁移到干净基线**——业务表全 0，sys_user 6、department 68；`uploads/` 0 文件
- 环境：MySQL80 Running(3306)；`10.31.67.107`(WLAN, /16)；无 Docker；非管理员（防火墙/进程管理受限）

## 本轮改动（2026-10-17）

| 文件 | 一句话 |
|---|---|
| `deploy/start-desk.ps1` | 一键启动：端口检查→内网 IP 探测→JDK17 实测校验→启动→轮询 200 才算 READY |
| `deploy/stop-desk.ps1` | 按命令行匹配 jar 停止，不误杀 IDEA |
| `deploy/README.md` | 部署手册：形态/环境变量/手机验收/防火墙/自启/已知限制/上云缺口 |
| `deploy/qr/{report,today,login}.png` | 三端入口二维码（240×240，已验证可扫） |
| `.gitignore` | 加 `deploy/logs/` |
| 记忆 4 文件 | findings 加 D-028、progress 加 S-039、task_plan 改 Next Step、本文件重写 |

## Next Step（从这里开始）

> 唯一主线：把"手机能打开"收口，其余后置

1. 防火墙放行（**管理员**）：`netsh advfirewall firewall add rule name="ITSD-8080" dir=in action=allow protocol=TCP localport=8080` → `deploy/README.md` 第五节
2. 端口归一：停 IDEA 的 8080 实例 → `deploy\start-desk.ps1 -Port 8080` → 扫 `deploy/qr/report.png`
3. 手机实测 `http://10.31.67.107:8080/report.html`（**唯一有效验收**）
4. 开机自启/服务化 → `deploy/README.md` 第六节
5. git 提交 `deploy/` + 记忆文件

## 不能碰的约束（必读）

- **.ps1 必须 UTF-8 with BOM**：PS 5.1 按 GBK 读，中文注释会截断字符串；`edit` 工具会丢 BOM，改完要补 → `findings.md#D-028`
- **JAVA_HOME 默认 jdk8**：必须实测 `java -version` ≥17；`java -version` 走 stderr，函数内要临时 `ErrorActionPreference=Continue` → `D-028`
- **mvn 参数要加引号**：`"-Dmaven.repo.local=.m2repo"`，否则 PowerShell 在 `-Dmaven` 处切分 → `progress.md#S-039`
- **沙箱内 CIM 不可用**（`Get-NetTCPConnection`/`tasklist`/`Get-CimInstance` 静默失败）→ 端口检查用 `netstat -ano`；进程只能看到自己起的 → `D-028`
- **内网 IP 探测要排除虚拟网卡**（WSL/Hyper-V/VMware），否则拿到 `172.23.240.1` → `D-028`
- **同机访问内网 IP 成功 ≠ 手机能访问**（不经入站防火墙），手机实测才算过 → `deploy/README.md`
- **http 下 PWA 不可用**（SW 需安全上下文），页面有守卫不会报错，别当 bug 修 → `D-028`
- **8080 是用户的 IDEA 实例，不要从沙箱强杀** → `progress.md#S-039`
- 取真实行数用 `COUNT(*)`，`information_schema.table_rows` 是估算 → `D-028`
- Git commit/push 由**用户手动**完成；科室端接口用 `departmentId`；记忆规范 findings/progress 只追加 → `findings.md#D-001`/`#D-002`
- 报销单已作废/退回后工单回池可重勾；设备照片功能已移除勿加回 → `progress.md#S-031`

## 数据与环境事实

- 演示账号（BCrypt，见 `data.sql`）：`admin/admin123`、`dispatcher/123456`、`repairer1|repairer2/123456`、`reporter1/123456`、`finance/123456`
- 迁移前备份：`.rdoc_inspect/backup/pre-deploy-20261017-143714.sql`（19,227 B，`Dump completed` 已校验）
- 签名图归档：`.rdoc_inspect/backup/uploads-pre-deploy-20261017-143731/`（8 张，108 KB）
- 旧备份：`.rdoc_inspect/backup/business-data-20261016-212234.sql`、`orphan-uploads-20261016-212234/`
- 日志：`deploy/logs/desk-<ts>.out.log`（已 gitignore）
- 企微通知配置位：`application.yml` 的 `notify.wecom.webhook`（当前空=关闭）+ `NOTIFY_PUBLIC_HOST` → `progress.md#S-026`

## 回读顺序（细节不够时按此顺序）

1. `findings.md#D-028`（部署决策 + 5 个环境坑）
2. `progress.md#S-039`（本轮实测数据与错误修复表）
3. `deploy/README.md`（可执行步骤全在这）
4. `git log --oneline -6`（HEAD=`3fc4d73`）

## 建议技能

- project-memory：恢复/落盘项目状态
- diagnose-windows-sandbox-acl：沙箱写/读异常时排查 ACL
