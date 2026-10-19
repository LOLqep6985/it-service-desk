# 多院区现场 IT 运维工单与调度系统

面向多院区现场 IT 运维场景的 Web 系统原型，覆盖科室报修、管理员派单、维修人员处理、科室验收和月度报销等环节。项目用于验证从报修到财务回填的业务流程；当前部署目标为院内局域网单机运行，不代表已在真实生产环境上线。

## 项目状态

- 核心业务闭环已实现：科室提交报修 → 管理员派单 → 维修人员接单和维修 → 科室签字验收 → 管理员核价并按月生成报销单 → 科室签字 → 财务回填。
- 已实现科室三级选择（院区、楼栋、科室）、PC 管理后台、移动端页面、角色权限控制、工单访问控制、统计概览、应用内通知轮询和二维码入口。
- 已有院内单机部署脚本及部署说明。项目记录中的手机端验收仍待完成；防火墙放行、端口归一和手机实测不能视为已完成。
- 暂无公网云部署、HTTPS、无人值守服务守护或正式生产运行承诺。

## 业务流程

```text
科室报修端
    │ 提交报修单
    ▼
管理员后台 ──派单──> 维修人员工作台
                         │ 接单、维修记录、耗材
                         ▼
                    科室验收签字
                         │
                         ▼
管理员核价并生成月度报销单
    │                    │
    │ 财务回填 <── 科室签字
    ▼
   已报销
```

报修单与工单是不同业务对象：报修单由科室提交；管理员派单后生成分配给维修人员的工单。工单主要状态包括 `ASSIGNED`、`IN_PROGRESS`、`PENDING_ACCEPT`、`COMPLETED`；报销单状态包括 `PENDING_SIGN`、`PENDING_REIMBURSE`、`REIMBURSED`。

## 主要功能

| 端 | 当前实现 |
|---|---|
| 科室报修端 | 按院区、楼栋、科室三级选择并绑定科室；提交报修；查看本科室相关记录；处理待验收工单和报销单签字。 |
| 管理员/调度端 | 查看待受理报修并派单；管理工单和报销流程；查看统计概览、科室和用户等信息。 |
| 维修人员端 | 查看任务、接单、记录维修过程和耗材、提交科室验收。 |
| 通知 | 应用内通知按当前实现的事件生成并由页面轮询获取；不是企业微信、短信或锁屏 Web Push 推送。 |
| 文件与签名 | 文件由应用上传并保存到配置的上传目录；科室验收和报销签字流程使用签名记录。 |

> 当前版本不提供设备照片报修功能。工作台及相关页面以实际部署版本为准。

## 技术栈

- Java 17、Spring Boot 2.7.18
- MyBatis-Plus 3.5.7、MySQL 8
- 前端：原生 HTML、CSS、JavaScript；后端应用直接提供静态页面
- Maven 构建；院内单机部署使用 Spring Boot 可执行 JAR 和 PowerShell 启停脚本

项目当前未使用 Vue、Redis、Docker 或 Nginx 作为运行依赖。

## 运行方式

### 环境要求

- JDK 17
- Maven
- MySQL 8

### 配置数据库

创建数据库，并准备一个应用专用账号。账号密码由本机环境配置，不要将真实凭据提交到仓库：

```sql
CREATE DATABASE it_service_desk
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
```

应用通过 `DB_URL`、`DB_USERNAME`、`DB_PASSWORD` 配置数据库连接；上传目录可通过 `FILE_UPLOAD_DIR` 配置，监听端口可通过 `SERVER_PORT` 配置。若未设置，应用会使用 `application.yml` 中的本地开发默认值。部署或共享环境应显式设置自己的凭据，不要沿用演示默认值。

### 构建与启动

在项目根目录执行：

```powershell
$env:JAVA_HOME = "<JDK 17 安装目录>"
$env:DB_URL = "jdbc:mysql://localhost:3306/it_service_desk?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true"
$env:DB_USERNAME = "<数据库用户名>"
$env:DB_PASSWORD = "<数据库密码>"
mvn clean package
java -jar target/desk-0.1.0.jar
```

项目启动时会执行 `schema.sql` 和 `data.sql` 中的初始化语句。请在个人或隔离的演示数据库中运行；不要直接对含有重要数据的数据库执行初始化或迁移操作。

页面入口：

- 登录与管理后台：`http://localhost:8080/login.html`
- 科室报修端：`http://localhost:8080/report.html`
- 维修人员任务页：`http://localhost:8080/today.html`

演示账号数据由 `data.sql` 初始化。为避免在仓库文档中传播密码，账号密码不在此列出；请在本地演示环境的系统信息页或独立的安全渠道确认演示凭据。不要将生产账号或密码写入 README、截图或提交记录。

> 本项目使用 MySQL，不是仅用内存的演示版本。沙箱内 Maven 构建若需使用项目缓存仓库，可按项目环境说明指定 `-Dmaven.repo.local=.m2repo`；IDEA 与命令行的 Maven 仓库设置可能不同。

## 院内单机部署

项目提供 `deploy/start-desk.ps1`、`deploy/stop-desk.ps1` 和独立部署手册。目标形态是在一台院内可访问的 Windows 主机上运行 JAR 和 MySQL，手机通过同一院内网络访问。部署细节、验收步骤和限制见 [`deploy/README.md`](deploy/README.md)。

当前应按“部署脚本和本机运行已准备，手机端实测待完成”理解部署状态。手机访问还依赖主机防火墙规则、正确端口和二维码地址；局域网 HTTP 不具备 HTTPS 安全上下文，Service Worker 离线能力不可用。主机休眠或关闭也会导致服务不可用。

## 测试

仓库包含针对签字类型和工单访问控制的单元测试。运行：

```powershell
mvn test
```

这部分测试不等同于完整的浏览器端到端测试，也不证明部署网络、手机浏览器或生产环境已经验收。演示前应在目标环境按业务闭环实际操作并记录结果。

## 目录结构

```text
src/main/java/com/itservice/desk/
├── controller/   REST API 与页面入口
├── service/      业务逻辑
├── mapper/       MyBatis-Plus 数据访问
├── model/        实体与枚举
├── dto/          请求/响应对象
├── config/       Web 配置
└── util/         通用工具
src/main/resources/
├── static/       管理后台、报修端、维修端及静态资源
├── schema.sql    表结构初始化
├── data.sql      演示账号与科室初始化数据
└── application.yml
src/test/          单元测试
sql/               数据库迁移脚本
deploy/           院内单机启停脚本、二维码与部署手册
```

## 已知限制与后续事项

- 院内手机端联网与完整业务流程实测尚待完成；具体当前状态以实际验收记录为准。
- 当前部署面向局域网 HTTP，不提供公网访问、HTTPS 或离线 Service Worker 能力。
- 单机进程异常后的自动恢复和开机自启尚未完成配置；主机需要保持运行。
- 数据库默认值仅适用于本地开发，不应直接用于共享或生产环境；上线前应更换凭据并评估数据安全和使用授权。
- 手机锁屏弹窗、短信/企业微信等外部推送不在当前实现范围内。
- 报销导出、服务端分页及科室/用户增删改等能力尚未纳入当前范围。

## 项目题目

`[待补充:拟定的毕业设计题目]`

题目及毕业设计范围尚待与指导老师沟通确认。
