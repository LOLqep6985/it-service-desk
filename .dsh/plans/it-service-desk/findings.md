# 决策与踩坑记录：it-service-desk

## 决策记录

（保留既有决策：技术栈 Java 全栈 / Redis 可降级 / AI 后置 / 项目定位升级 / 第一版范围 / 网络异常策略 / 先 M0 后 7 天）

### 2026-10-13：M0 采用内存存储（InMemoryStore）

- 状态：accepted
- 来源：开发过程发现（环境限制）
- 决策：M0 数据层用 ConcurrentHashMap 内存存储，不接数据库
- 理由：本地无任何 JDBC 驱动缓存（mysql/h2/sqlite 均无），且 Maven 中央仓库/阿里云镜像网络不可达（HTTP 000），无法下载驱动
- 后果：M0 仅验证业务闭环；数据重启丢失可接受；Day 1 需解决真实数据库

### 2026-10-13：Maven 离线构建的三个环境坑

- 状态：accepted
- 来源：开发过程踩坑
- 坑 1：实际本地仓库是 D:\Maven\repository（用户 settings.xml 指定），不是 C:\Users\DELL\.m2\repository
- 坑 2：_remote.repositories 元数据引用旧仓库 id（nexus-aliyun）导致离线模式不认缓存 → 解法：删除全部 _remote.repositories（共 1653 个），不影响 jar
- 坑 3：JAVA_HOME 指向 jdk8，导致「无效的目标发行版:17」→ 解法：构建时设置 JAVA_HOME=D:\DevTools\Java\jdk17
- 坑 4：Spring Boot 2.7.18 默认管理 lombok 1.18.30，本地缓存 1.18.46 → 解法：pom.properties 覆盖 <lombok.version>1.18.46</lombok.version>

### 2026-10-13：M0 验证结论

- 状态：accepted
- 来源：M0 测试结果
- 决策：M0 垂直切片可行，进入 7 天完整开发
- 验证证据：curl 闭环测试 7 项全部通过；登录/创建/今日任务/详情/维修关闭/页面 200

## 踩坑记录

| 日期 | 问题 | 原因 | 解决方案 |
|---|---|---|---|
| 2026-10-13 | Maven 离线不认缓存 POM | _remote.repositories 引用旧 repo id | 删除全部 _remote.repositories 元数据 |
| 2026-10-13 | 无效的目标发行版:17 | JAVA_HOME 指向 jdk8 | 构建时设 JAVA_HOME=D:\DevTools\Java\jdk17 |
| 2026-10-13 | lombok 1.18.30 缺失 | 缓存版本为 1.18.46 | pom 覆盖 lombok.version=1.18.46 |
| 2026-10-13 | MySQL/MyBatis-Plus 依赖不可用 | 网络封锁 maven 仓库 | 待 Day 1 解决（找镜像/热点/手动安装 jar） |
### 2026-10-13：ORM 改用 Spring Data JPA（替代 MyBatis-Plus）

- 状态：accepted
- 来源：开发环境限制 + 工程决策
- 决策：Day 1 起使用 Spring Data JPA（spring-boot-starter-data-jpa 2.7.18 已缓存）作为 ORM，弃用 MyBatis-Plus
- 理由：
  - 本地 Maven 仓库已缓存完整 JPA 依赖树（Hibernate 5.6.15 + HikariCP 4.0.3 + mysql-connector-j 9.7.0）
  - MyBatis-Plus 未缓存，且 Maven 中央仓库/阿里云镜像网络不可达，无法下载
  - JPA 是 Java 企业级标准 ORM，简历上同样有说服力，面试可解释「为什么选 JPA：声明式查询、实体映射规范、跨实现可移植」
- 后果：pom 需覆盖 mysql.version=9.7.0（Spring Boot 2.7.18 默认管理 8.0.33，本地缓存为 9.7.0）

### 2026-10-13：MySQL 本机环境状态

- 状态：进行中
- MySQL80 服务当前为停止状态；沙箱无权限启动（需管理员）
- 待用户：以管理员启动 MySQL80，并准备数据库 it_service_desk 及账号
### 2026-10-13：Maven 网络问题彻底解决（Clash 代理 + 项目内本地仓库）

- 状态：accepted
- 来源：排查发现
- 根因：沙箱禁止写入 D:\Maven\repository（AccessDeniedException），并非代理/镜像配置问题
- 解法：
  - 构建时设置代理环境变量：HTTP_PROXY / HTTPS_PROXY = http://127.0.0.1:7890（Clash）
  - 使用项目内本地仓库：-Dmaven.repo.local=.m2repo（已 gitignore）
  - Maven 即可通过 aliyunmaven 镜像正常下载依赖（已验证 mybatis-plus-boot-starter 3.5.7 下载成功）
- 注意：用户自己的终端/IDE 仍可用系统仓库 D:\Maven\repository（沙箱外无写限制）

### 2026-10-13：ORM 决策回退为 MyBatis-Plus（弃用 JPA 方案）

- 状态：accepted（用户明确要求）
- 来源：用户要求「先解决 MyBatis-Plus 依赖，再用 MyBatis-Plus 完成主项目」
- 决策：主项目使用 MyBatis-Plus（mybatis-plus-boot-starter 3.5.7），不用 Spring Data JPA
- 理由：用户偏好 + 与简历技能栈匹配；依赖问题已解决，不再需要 JPA 降级
- 后果：pom 添加 mybatis-plus-boot-starter 依赖；M0 内存存储将替换为 MyBatis-Plus 数据访问层
### 2026-10-13：IDEA 环境确认（Maven + JDK17 + 项目名/包名）

- 状态：accepted
- 来源：用户确认 + 上传的 Maven 位置描述
- 决策：
  - 项目名保留 it-service-desk；包名保留 com.itservice.desk
  - IDEA Maven：home D:/DevTools/Maven（3.9.16）、User settings C:\Users\DELL\.m2\settings.xml、Local repository D:\Maven\repository（由 settings.xml 决定，IDEA 不勾 Override，正确）
  - Use settings from .mvn/maven.config：已勾选（本项目无 .mvn/maven.config，无影响）
  - JDK 17 已选为 SDK
- 备注：沙箱内 CLI 构建继续用项目内 .m2repo（沙箱无权限写 D:\Maven\repository）；IDEA 构建用 D:\Maven\repository，二者互不冲突
### Maven 双仓库说明（重要，防止后续出错）

- 状态：accepted
- 来源：项目开发过程中的环境事实
- **IDEA 用系统仓库 D:\Maven\repository**（IDEA 无沙箱限制，能正常写入；由 settings.xml 的 localRepository 指定）
- **沙箱 CLI 用项目内 .m2repo**（沙箱没权限写 D:\Maven\repository，所以构建时用 -Dmaven.repo.local=.m2repo + 代理环境变量 HTTP_PROXY/HTTPS_PROXY=http://127.0.0.1:7890）
- **两者互不冲突**：.m2repo 已 gitignore；依赖版本一致（都从阿里云镜像解析）
- 注意：如果后续想在沙箱里直接把依赖写到 D:\Maven\repository，需要每次构建时给「完整权限」授权，但没必要，保持现状最省事
- 关联：IDEA 里项目可直接 Reload Maven；沙箱里构建命令见 task_plan 备注
### 2026-10-13：Day 1 技术要点

- MyBatis-Plus 3.5.7 + mysql-connector-j（Spring Boot 2.7.18 管理版本 8.0.33）在 MySQL 8.0.42 上运行正常
- 表名：sys_user / department / work_order；字段驼峰↔下划线自动映射（map-underscore-to-camel-case）
- 枚举（UserRole/Campus/WorkOrderStatus）默认按枚举名存储为 VARCHAR，与 data.sql 种子的英文值一致
- spring.sql.init.mode=always 每次启动执行 CREATE TABLE IF NOT EXISTS + INSERT IGNORE，幂等安全
- mysql 命令行客户端显示中文乱码是 Windows 控制台代码页问题（GBK），数据本身为 utf8mb4 正确
- 遗留：暂未加唯一约束之外的索引；Day 2 将补 JWT 鉴权与 RBAC
### 2026-10-13 修复：中文乱码（寮犲伐=张工）

- 现象：IDEA 中登录后界面显示「寮犲伐（维修工）」（UTF-8 字节被按 GBK 读取后入库导致）
- 根因：Spring Boot spring.sql.init 在中文 Windows 上默认用平台编码（GBK）读取 UTF-8 的 data.sql，种子数据中文被错误解码后写入 MySQL
- 修复：application.yml 增加 spring.sql.init.encoding: UTF-8；并 DROP 旧表让 schema.sql/data.sql 重新初始化
- 验证：应用接口与 MySQL 中中文均正确（系统管理员/张工/王护士等）

### 2026-10-13 修复：IDEA 启动失败 Error starting ApplicationContext

- 现象：用户 IDEA 启动报 Error starting ApplicationContext / LoggingFailureAnalysisReporter
- 根因：沙箱后台启动的 java 进程残留占用 8080 端口（job_kill 只取消 job 包装，未强制杀 java）
- 修复：强制结束残留 java 进程；后续沙箱验证完应用务必 Stop-Process 清理
- 教训：后台跑 java -jar 后，结束任务要用 Get-Process java + Stop-Process -Force 兜底
### 三端系统需求总结（来自用户第二个分享链接的 5 个提问）

- 状态：accepted（用户认可为整个项目流程的总结）
- 提问1：故障信息太冗杂，需精简。
- 提问2：报修字段极简——只保留 科室、报修人、宽泛设备名称、设备照片、简单问题描述；删除具体地点、优先级、复杂描述。
- 提问3：维修/耗材更换每次打欠条、每月开票报销；科室负责人=护士长（报销签字）；报修完成后需护士长/指定人签字确认。
- 提问4：科室端免登录，以科室名义报修，只需填报修人姓名；科室负责人填科室主任或护士长。
- 提问5：报修单由科室提交，管理员看到后分派给维修人员成为工单；科室专用工作手机绑定科室；系统是三端（管理员后台端/维修人员端/科室报修端）。
- 影响：现有 work_order 需演进为「报修单 + 工单」分离模型；前端需新增科室免登录报修端与管理后台端。
### 权限控制（「+报修」不是隐藏，而是没有权限）

- 状态：accepted（用户纠正）
- 决策：前端隐藏按钮只是 UI 层；后端必须做权限校验
- 实现：所有 /api/work-orders 接口要求 X-Token 头（登录返回 token-{userId}）
  - 创建工单：仅 REPORTER（报修人），否则 403
  - 提交维修：仅 REPAIRER（维修工），否则 403
  - 今日任务/详情：登录即可（token 识别当前用户，不再信任 query 参数 userId）
  - 无 token：401
- 前端 create.html / today.html / detail.html 已改为携带 X-Token
- 验证矩阵 7 项全部通过（见 progress）
### 2026-10-13：Day 2 技术要点

- report 表用 report_no（R000001 格式）作为业务编号，插入后根据自增 id 更新生成
- 派单时从 report 复制信息生成 work_order（deviceName 作为 title，departmentHead 作为联系人），并回写 report.status=DISPATCHED
- work_order 新增 report_id 关联、sign_status/signed_by/signed_at 为签字确认预留
- consumable/reimbursement/department_binding 表已建但 Day 2 未接 API（Day 3/5/6 使用）
- 兼容性：旧 work_order 数据（report_id=NULL）不受影响
### 2026-10-13：Day 3 技术要点

- 免登录报修：通过 department_binding.device_code 识别科室，报修单 creator_id=null、binding_id 记录设备
- 文件上传：保存到 uploads/（gitignore）；访问用控制器 @GetMapping /uploads/{filename:.+} 而非 WebMvcConfigurer 资源映射（后者在本环境 404）
- 权限：绑定设备仅 ADMIN/DISPATCHER；免登录报修/历史查询以 device_code 为凭据（设备码即弱凭证，Day6 可加 PIN）
- 报修单历史按 department_id 查询，非严格按设备，便于科室共用
### 2026-10-13：科室报修页 Bug 修复要点

- Jackson 默认把枚举序列化为名字符串（如 "PENDING"），前端不能用 .label；需前端维护 label 映射或后端返回对象
- 防重复提交：report 表按 department_id + device_name + created_at >= now-10min 查询最近记录，非 CANCELLED 即拒绝（登录式与免登录式都加）
- department 表新增 department_head 字段（默认科室负责人），免登录报修自动带出并只读
- report.html 提交成功按钮禁用 10 分钟倒计时，防手抖
### 2026-10-13：Day 4 技术要点

- 用户列表接口不返回密码（置 null）
- admin.html 用纯 HTML+JS 实现，无前端框架；派单通过弹窗选择维修工
- 报修单历史中已派单/已取消用不同 badge 色
- 基础数据只读展示科室与用户，管理编辑（增删改）留待后续
### 2026-10-13：Day 5 技术要点

- 接单/维修归属校验：通过 WorkOrderService.getById 校验 assigneeId == 当前维修工，防越权处理他人工单
- RepairRequest 扩展 List<ConsumableItem>，前端动态行收集耗材/欠条
- 工单完成后 sign_status 保持 UNSIGNED（待签字），等待 Day 6 签字闭环
- consumable 表启用：记录耗材名/数量/单价/是否欠条/欠条编号，作为月度报销数据源
### 2026-10-13：管理员后台工单展示补全

- 教训：管理员后台不仅要有报修单管理，还要有工单管理（派单后的执行状态）
- 实现：WorkOrderController 增加 GET /api/work-orders（listAll），WorkOrderService.listAll(status) 支持按状态过滤
- admin.html 增加工单管理表，维修工姓名通过 users 接口映射（assigneeId → name）