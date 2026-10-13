# 多院区现场 IT 运维工单与调度系统

把医院/企业的「微信群报修」升级为 **PC 管理调度后台 + 手机端现场维修工作台** 的移动优先 Web 应用。

- 科室报修端（免登录）：工作手机绑定科室，极简报修（报修人/设备/照片/描述），科室自动带出
- 管理员后台（PC）：待受理报修单派单、工单管理、耗材核价、月度报销单管理
- 维修人员端（手机）：今日任务、接单、到场维修、耗材登记、发起科室验收签字
- 报销闭环：护士长/主任手写签名验收（可代签）→ 管理员核价 → 按月生成报销单 → 科室签字 → 财务回填

## 技术栈

- Java 17 / Spring Boot 2.7.18
- MyBatis-Plus 3.5.7 / MySQL 8
- 前端：原生 HTML + JS（H5，移动优先），Vue3 预留
- Maven 多仓库：IDEA 用系统仓库，沙箱 CLI 用项目内 `.m2repo`

## 核心流程与状态机

```
报修单(R000xxx) → 管理员派单 → 工单(ASSIGNED → IN_PROGRESS → PENDING_ACCEPT → COMPLETED)
                                        ↑ 维修工提交待验收     ↑ 科室手写签名
报销单(PENDING_SIGN → PENDING_REIMBURSE → REIMBURSED)
       ↑ 按月汇总已签字工单      ↑ 科室签字     ↑ 财务回填
```

## 快速启动

1. 准备 MySQL 8，创建数据库与账号（默认 `it_service_desk` / `itsd` / `itsd123456`）：
   ```sql
   CREATE DATABASE it_service_desk DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_unicode_ci;
   CREATE USER 'itsd'@'localhost' IDENTIFIED BY 'itsd123456';
   GRANT ALL PRIVILEGES ON it_service_desk.* TO 'itsd'@'localhost';
   FLUSH PRIVILEGES;
   ```
2. 可通过环境变量覆盖数据库连接（默认值见 `application.yml`）：
   ```bash
   export DB_URL="jdbc:mysql://localhost:3306/it_service_desk?..."
   export DB_USERNAME=itsd
   export DB_PASSWORD=itsd123456
   ```
3. 构建并运行：
   ```bash
   mvn clean package -DskipTests
   java -jar target/desk-0.1.0.jar
   ```
4. 访问：
   - 登录：`http://localhost:8080/login.html`
   - 科室免登录报修：`http://localhost:8080/report.html`（设备码 `DEV001`）

## 演示账号

| 角色 | 账号 | 密码 |
|---|---|---|
| 管理员 | admin | admin123 |
| 调度员 | dispatcher | 123456 |
| 维修工 | repairer1 / repairer2 | 123456 |
| 报修人 | reporter1 | 123456 |

科室设备码：`DEV001`（科室：门诊楼-三层）。

## 目录结构

```
src/main/java/com/itservice/desk
├── controller   # REST API（auth/report/work-order/reimbursement/...）
├── service      # 业务逻辑
├── mapper       # MyBatis-Plus Mapper
├── model        # 实体与枚举
├── dto          # 请求/响应对象
└── config       # 配置
src/main/resources
├── static       # 前端 H5 页面（admin/report/today/detail/login）
├── schema.sql   # 建表（启动自动执行，幂等）
└── data.sql     # 种子数据
```

## 说明

- 本科毕业设计/简历项目；数据库连接为本地演示默认值，生产环境请通过环境变量注入。
- `uploads/`（照片与手写签名）已被 `.gitignore` 排除，不进入版本库。