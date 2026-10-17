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
### 2026-10-13：Day 6-1 技术要点

- 工单详情通过多个接口组合：/api/work-orders/{id} + /api/reports/{reportId} + /api/work-orders/{id}/consumables
- 前端用右侧 Drawer（transform translateX）避免打断列表；照片 window.open 放大
- 空值统一兜底「未填写/暂无数据」，杜绝 undefined
### 2026-10-13：Day 6-2 技术要点

- 签字采用轻量方案：科室端免登录（deviceCode）查看本科室待验收工单并手写签名（Canvas 导出 PNG 上传）
- 代签：signedBy 存「被代签人(由代签人代签)」，不新增字段
- 核价：管理员在工单详情抽屉直接编辑耗材实际单价（unit_price），第一版不引入独立采购角色
- 状态机：ASSIGNED → IN_PROGRESS → PENDING_ACCEPT（维修工提交）→ COMPLETED（签字后）
- 欠条字段（is_iou/iou_no）保留在表，但 UI 不再展示（符合「第一版不做欠条」）
- 测试注意：科室端待验收按工单 department_id 过滤，必须用绑定同一科室的报修单测试（DEV001=科室1）
### 2026-10-13：uploads 路径踩坑

- 现象：沙箱上传的图片，IDEA 运行（不同工作目录）时 /uploads/xxx 404
- 根因：file.upload-dir 相对路径依赖进程工作目录（user.dir）
- 解法：改为绝对路径 ，环境变量可覆盖，保证跨运行环境一致
- uploads/ 已在 .gitignore，不污染仓库

### 2026-10-13：Day 6-4 月度报销单技术要点

- 报销单模型：reimbursement（月份/科室/合计/状态/签字字段）+ reimbursement_item 关联工单（多对多快照）
- 生成逻辑：按 signed_at 月份筛选 COMPLETED+SIGNED 工单 → 按科室分组 → 合计耗材金额（unit_price × quantity）→ 同一科室同月只生成一次（幂等）
- 状态机：PENDING_SIGN（待护士长签字）→ PENDING_REIMBURSE（已签字待报销）→ REIMBURSED（财务回填）
- 科室端签字复用 Canvas 手写签名弹窗：新增报销单模式（隐藏代签），与工单验收签字共用上传/确认逻辑
- 权限：生成/列表/详情/财务回填 仅 ADMIN/DISPATCHER；科室端凭 deviceCode 查本科室报销单并签字
- 前端：admin.html 新增「月度报销单」区块（月份选择/生成/列表/详情抽屉/财务回填）；report.html 新增报销单卡片（签字确认/查看明细）
- 测试注意：同一月份生成后，新增签字工单不会自动加入已生成报销单（快照语义）；如需补录需调整逻辑
- MySQL 不支持 ALTER TABLE ADD COLUMN IF NOT EXISTS：新增列采用「DROP 空表重建 + CREATE TABLE IF NOT EXISTS」方案（reimbursement 原为空表，无数据风险）

### 2026-10-13：推送云端前安全处理

- 状态：accepted（用户明确要求）
- 决策：推送 Gitee 前，把 application.yml 的数据库连接改为环境变量占位符 `${DB_URL:...}` / `${DB_USERNAME:itsd}` / `${DB_PASSWORD:itsd123456}`
- 理由：本地默认值不变（clone 即跑），云端/生产可通过环境变量注入，避免硬编码敏感信息
- 补充：仓库仍含演示账号密码（admin123 等）与本地数据库默认密码，属演示数据，公开仓库可接受；如需更严格可后续再清理

### 2026-10-13：Git 工作流变更——由用户手动 commit/push

- 状态：accepted（用户明确要求）
- 决策：此后 AI 不再直接执行 git commit / git push；每次完成代码变更并落盘项目记忆后，AI 只提供「修改摘要 + 建议提交信息（Conventional Commits）」，由用户在 VSCode（Git Graph 扩展）中手动 commit 并 push 到云端仓库
- 理由：用户希望在 VSCode 中直观查看变更（Git Graph），自己掌控版本历史与推送时机
- 影响：AGENTS.md「强制规则」已同步更新；AI 侧仍负责更新 .dsh/plans 下的进度文档，但提交动作留给用户

### 2026-10-13：PWA 化与科室端首次绑定优化（体验优化）

- 状态：accepted（用户选定方案 A：PWA，不做 APK）
- 决策：科室端与维修端采用 PWA「添加到主屏幕」，不引入 Android 工程/APK（避免过度设计与版本分发负担）
- 实现：
  - 新增 `manifest-report.json`（科室报修，绿色 #27ae60）与 `manifest-repairer.json`（维修工作台，蓝色 #1d6fe0），`start_url` 分别指向 `/report.html` 与 `/today.html`
  - 新增 `service-worker.js`：导航网络优先、失败回退缓存；静态资源缓存优先；`/api/` 与 `/uploads/` 不缓存
  - 图标：PowerShell + System.Drawing 生成 `icons/report-{192,512}.png`（绿底「报」）与 `icons/repair-{192,512}.png`（蓝底「修」）
  - report/today/detail/login 四页接入 manifest + theme-color + apple-touch-icon + SW 注册
  - 科室端绑定优化：`?deviceCode=` URL 参数自动绑定（为扫码入口预留）、「重新绑定」按钮、绑定成功直接展示全部卡片
- 踩坑：Spring Boot 把 `.webmanifest` 以 `application/octet-stream` 返回，浏览器可能拒绝 → 改名为 `.json` 后正确返回 `application/json`
- 注意：Service Worker 只在 HTTPS 或 localhost 下生效；局域网 http 部署时页面仍可正常使用，仅离线/安装能力不可用（后续 Day7 部署建议上 HTTPS）

### 2026-10-13：需求变更——去掉设备码，科室端改为「院区→科室」两级下拉

- 状态：accepted（用户明确要求）
- 决策：
  - 废弃科室工作机的「设备码绑定」模式，改为**下拉选择科室**绑定（先选院区，再选该院区下的科室）
  - 科室数据源使用用户提供的《医院科室楼层索引.md》（河南省人民医院豫东分院：东区/老院/西院，共 68 个科室）
- 实现：
  - department 表导入 68 个真实科室（name 唯一键 + INSERT IGNORE 幂等）；命名规则「建筑-楼层-科室」，如「门诊楼-三层-五官科诊室」
  - 后端接口全面支持 `departmentId`（匿名报修/报修历史/待验收/已完成/详情/耗材/签字/报销单），保留 `deviceCode` 兼容旧方式
  - report.html 绑定页改为两级下拉：院区（东区院区/老院区/西院区）→ 科室；绑定后存 localStorage（itsdDepartmentId/itsdDepartmentName）并刷新，显示「当前科室：XXX」
  - 错误提示改为「请选择科室」；科室负责人有默认值时只读，无默认值时可手动填写
- 踩坑/注意：
  - MySQL 不支持 ALTER ADD COLUMN IF NOT EXISTS，但支持 ADD UNIQUE KEY；已手动对现有库执行 `ALTER TABLE department ADD UNIQUE KEY uk_dept_name (name)`，并清理旧演示科室数据
  - 旧演示数据（work_order/report）仍引用已删除的旧科室 id，但表内已有科室名快照，展示不受影响；新科室 id 从 6 开始（AUTO_INCREMENT 未重置）
  - `/api/reimbursements/by-department` 新增为语义化端点，`/by-device` 保留兼容

---

> 以下条目按 project-memory **v3.0.0** 规范记录（事实层只追加，历史条目保持原样）。

## 决策记录（v3 编号）

### D-001 | 2026-10-14：采用 project-memory v3 规范管理项目记忆

- 状态：accepted
- 来源：用户明确要求
- 背景：用户用 DeepSeek V4.1 flash 修改了 project-memory skill 为 v3.0.0；旧版 v2.0.0 已备份到 `D:\DeepSeek_data\gradaute_space\skills\_backup_project-memory_v2`（并已压缩存档 `.zip`）
- 决策：将新版 skill 复制到 `D:\DeepSeek_data\gradaute_space\skills\project-memory`；此后项目记忆按 v3 规范维护——`findings.md` / `progress.md` 只追加，`handoff.md` 为指针式交接单
- 理由：抗静默压缩的信息衰减；关键约束与用户原话必须逐字保留在磁盘
- 后果：findings/progress 的历史条目保持原样，新条目带 ID（D-/S-/P-）；handoff 重写为指针式
- 用户原话：> "你现在将新的skill复制到D:\DeepSeek_data\gradaute_space\skills目录下，并使用新的skill来保存工作进度。"

### D-002 | 2026-10-14：科室端去掉设备码，改为「院区→科室」两级下拉（补录用户原话）

- 状态：accepted（用户明确要求）
- 来源：用户明确要求
- 背景：科室工作机由医院信息科统一下发，逐个绑定设备码不现实；改为按科室绑定
- 决策：科室端绑定从「设备码输入」改为「先选院区 → 再选科室」两级下拉；科室数据源为用户提供的《医院科室楼层索引.md》68 个科室；绑定结果存 localStorage 并刷新显示「当前科室：XXX」
- 理由：一部工作机对应一个科室，按科室绑定更符合实际使用
- 后果：后端科室端接口全面支持 departmentId（保留 deviceCode 兼容）；department 表导入 68 个真实科室（详见上文 2026-10-13「需求变更——去掉设备码」条目）
- 用户原话：> "去掉设备码，改为直接下拉选择科室。" + "两个下拉列表，先选择院区，在选择科室" + "点击'绑定'后，把选中的科室名称存入浏览器的 localStorage，并刷新页面，页面上显示'当前科室：XXX'。"

### D-003 | 2026-10-14：项目现状核查（Day 7 启动前）

- 状态：accepted
- 来源：模型推断（全量读取代码 + git/服务状态核查，非用户指示）
- 事实（已核实）：
  - 远程仓库已从 Gitee 变为 **GitHub**：`it-service-desk` → `https://github.com/LOLqep6985/it-service-desk.git`；本地 `master` 与远程 `master` 同步（ahead/behind = 0/0）
  - 项目记忆 4 个文档已在 `a74be2b` 提交；工作区仅剩 `handoff.md` 的一处陈旧条目（本次已修正）
  - 环境可用：MySQL80 服务 Running；8080 端口空闲；`java -version` = 17.0.12；`D:\DevTools\Java\jdk17` 存在
  - 当前仓库**无任何测试代码**（有 spring-boot-starter-test 依赖，`src/test` 不存在）；**无 Dockerfile / docker-compose / Nginx 配置**
- 已知代码缺口（待用户决定是否纳入 Day 7 或后续）：
  1. 报销单签字**签名图片未落库**：`report.html` 上传签名后调用 `/api/reimbursements/{id}/sign` 传了 `signatureUrl`，但 `ReimbursementService.sign` 忽略该参数，`reimbursement` 表也无 `signature_url` 列 → 报销单签字无留痕（工单签字正常）
  2. `today.html` / `detail.html` 的 `WORK_ORDER_STATUS` 映射缺 `PENDING_ACCEPT`，维修工看已完成流程的工单会显示英文原名（`admin.html` / `report.html` 已含）
  3. 「10 分钟防重复提交」按 `department_id + device_name` 判定，而科室端设备名已改为**下拉宽泛类型**（打印机/电脑…）→ 同科室 10 分钟内报修两台不同打印机（都选「打印机」）会被误判重复
  4. `WorkOrderController.detail` 对已登录用户未校验归属（任意登录角色可看任意工单详情）
  5. 密码明文存储（演示可接受，简历项目建议说明或改哈希）
- 影响：Day 7 若只做「部署+README+简历」，上述 1-3 建议至少修掉（都是演示时会被看到的点）；4-5 可写入 README 的「已知限制/后续优化」

### D-004 | 2026-10-14：科室端绑定改为三级联动（院区→楼栋→科室）+ department.floor 显式建模

- 状态：accepted（用户明确要求）
- 来源：用户明确要求
- 用户原话（逐字）：
  > "现在只有两级下拉列表，需要改成三级"
  > "当第一级（院区）改变时，清空第二、三级，并动态加载该院区下的楼栋；"
  > "当第二级（楼栋）改变时，清空第三级，并动态加载该楼栋下的科室；"
  > "第三级下拉框因为科室较多；楼层号（如 1F）和科室名称请用不同颜色或标签做视觉区分"
- 数据事实：department 表 68 个科室，院区下恰好各 2 个楼栋（东区=门诊楼 20 + 病房楼 19；老院=住院部1号楼 11 + 急救中心 12；西院=门诊 3 + 住院部 3），楼栋字段原本已存在，缺的是**楼层**
- 决策：
  1. `department` 新增 `floor VARCHAR(20)`（值如 `1F`…`12F`，与用户《医院科室楼层索引.md》一致），把楼层作为**显式字段**建模，不在前端猜字符串
  2. **`name` 不改**：它承担全局唯一键（uk_dept_name，`药房` 等重名靠「楼栋-楼层-」前缀区分），且 `report.department_name` / `work_order.department_name` 存的是名字快照——改 name 会让历史记录与新记录显示不一致
  3. 第三级用**自定义下拉面板**而非原生 `<select>`：原生 `<option>` 在移动端无法可靠着色，无法满足「楼层号与科室名不同颜色/标签」；面板中每行 = 蓝色楼层标签（`.floor-chip`）+ 深色科室名
  4. `/api/departments` 固定按 `campus → building → id` 升序返回，保证三级分组顺序稳定
- 落地文件：
  - `sql/2026-10-14-department-floor.sql`（新建，幂等 ALTER + 回填，供旧库升级）
  - `src/main/resources/schema.sql`（department 加 floor 列）
  - `src/main/resources/data.sql`（追加幂等回填 UPDATE，全新库自动补 floor）
  - `Department.java`（加 floor 字段）、`DepartmentController.java`（稳定排序）
  - `report.html`（三级联动 + `.floor-chip` 样式 + 联动清空逻辑）
- 验证证据（本次实测）：
  - 迁移：68/68 行回填成功；接口返回 `floor` 无缺失；排序稳定
  - 三级联动：Node + 最小 DOM 桩跑真实接口数据，A–F 全段 PASS（院区→楼栋 2 项、门诊楼 20 科室 20 标签、改院区后二三级清空复位、6 楼栋合计 68 科室 0 缺失）
  - 幂等回填：手工把 id=13 的 floor 置 NULL → 重启应用 → 自动恢复 `3F`
  - 页面 `/report.html` HTTP 200 且含三级结构与样式
- 附带发现（未改，待确认）：README 仍在写「设备码 DEV001」（`本地绑定` 已在上一轮改为按科室），属上一轮遗留的文档陈旧，Day 7 写 README 时一并更新

### D-005 | 2026-10-14：三级联动抽为共用组件 + create.html 接入 + 管理后台基础数据分类

- 状态：accepted（用户明确要求）
- 来源：用户明确要求
- 用户原话（逐字）：
  > "1.create.html（报修人登录后报修）还是平铺的 68 项科室下拉，没做三级联动，改"
  > "2.README 在Day 7 写 README 时我一并更新"
  > "我发现IT 运维工单 · 管理员后台下面的"基础数据"部分太乱，要将科室信息分类规整好。"
- 决策：
  1. **抽为共用组件**：`static/js/department-cascade.js`（`window.DeptCascade`：三级联动 `create()`、楼层/短名工具、`groupByLocation()` 分组）+ `static/css/dept-cascade.css`（`.picker*` / `.floor-chip`）。理由：三级联动逻辑约 90 行，`report.html` 与 `create.html` 都要用，复制两份会导致后续改动双写；抽为组件后三页共用同一份实现
  2. **create.html 接入三级联动**，并在选择科室时自动带出科室负责人（有默认值 → 只读；无默认值 → 可手填），与科室端行为一致
  3. **管理后台「基础数据」拆分并分类**：原先把 68 个科室名用「、」拼进一个单元格（用户反馈"太乱"）→ 改为「基础数据 · 科室索引」+「基础数据 · 用户」两块；科室按 **院区 → 楼栋 → 楼层** 分类（楼层用统一 `.floor-chip` 蓝色标签、科室名用浅灰 chip、每级显示数量，楼层按数字升序 1F→12F）；用户按角色彩色标签展示
  4. **Service Worker 缓存版本 `itsd-v1` → `itsd-v2`** 并把新资源加入 `APP_SHELL`。理由：SW 对静态资源是 cache-first，不升级缓存版本会一直吃旧文件、页面改动看不到（该踩坑已在 v1 时期出现过）
- 落地文件：新增 `js/department-cascade.js`、`css/dept-cascade.css`；改 `report.html`（改为调用组件）、`create.html`、`admin.html`、`service-worker.js`
- 验证证据：`node .rdoc_inspect/test-frontend.js`（Node + DOM 桩 + 真实接口数据）**34 项全部 PASS**，覆盖：
  - 语法：组件 + 7 个页面内联脚本全部通过
  - 组件：院区→楼栋→科室联动、二三级清空、20 科室 20 楼层标签、`3F` 标签、科室短名去前缀、6 楼栋合计 68 科室 0 缺标签、分组 3 院区/6 楼栋/68 科室、门诊楼楼层升序 1F→8F
  - report.html：选老院/急救中心 12 科室 → 绑定写 localStorage + 触发刷新
  - create.html：三级联动、负责人带出（有/无默认值两种）、提交 `departmentId` 正确且带 `X-Token`、**未选科室被拦截且不发请求**
  - admin.html：概要「68 个科室 · 3 个院区 · 6 个楼栋」、6 个楼栋分组、楼层标签、短名、各院区计数 39/23/6、角色标签、旧 `baseDataBody` 已移除
  - HTTP 层：`/css/dept-cascade.css`(text/css)、`/js/department-cascade.js`(application/javascript)、三个页面均 200
- 遗留：README 文案更新按用户要求**推迟到 Day 7** 一并处理（不再单独改）

### D-006 | 2026-10-14：管理后台信息架构重构方案（侧边栏 + 顶部导航 + 仪表盘）

- 状态：accepted（用户明确要求，且按用户要求"先出方案再动手"逐项确认）
- 来源：用户明确要求
- 用户原话（逐字，核心诉求）：
  > "目前这些模块全部被平铺放在了同一个大界面上，导致页面极其杂乱，毫无重点，不符合现代 B 端后台的设计规范。请帮我重构这个管理后台的首页布局。"
  > "请不要直接盲目修改代码。请按照以下步骤跟我交互：1 先给出一份重构后的【路由结构设计图】和【菜单树结构】。2 给出【首页概览 Dashboard 的区块分布草图】…3 确认无误后，列出需要新建或修改的文件清单。4 最后再依次生成代码（注意组件解耦，不要把所有代码写在一个文件里）。"
- 用户确认的四项选择（逐字选项）：A 单入口 admin.html + hash 路由 + 视图 JS 模块；"新增 /api/stats/dashboard 聚合接口"；"近 7 日趋势柱状图（纯 CSS，零依赖）+ 列表页搜索/分页"；"要，展示运行信息（版本/上传目录/演示账号/已知限制）"
- 开工方式（用户选择）：「按批次 1→4 依次做，每批结束汇报等我验证」
- 核代码时发现的事实（影响指标口径）：
  - `WorkOrderStatus.PENDING("待派单")` **从未被使用**：`ReportService.dispatch()` 直接创建 `ASSIGNED` 工单，流程无独立"受理"环节
  - 因此「待受理数」与「待派单数」是同一个数（`report.status = PENDING`）→ 方案里合并为一张卡
- 路由与菜单树（单入口 + hash）：
  - `#/dashboard` 工作台·首页概览
  - `#/reports/pending` 工单业务·待受理报修单（角标=待受理数）
  - `#/work-orders` 工单业务·工单管理（角标=进行中+待验收）
  - `#/reports/history` 工单业务·报修单历史
  - `#/reimbursements` 财务与报销·月度报销单（角标=待签字数）
  - `#/departments` / `#/users` / `#/system` 系统与基础数据配置
  - 选 hash 而非 History API 的理由：Spring Boot 静态直出、无服务端路由，`#/x` 刷新/分享/后退均不 404，零后端改动
- 指标口径（定稿）：今日新增报修单 / 昨日新增（环比）/ 待受理(=待派单) / 已派单待接单 / 处理中 / 待科室验收 / 本月已完成 / 本月报销金额与三种状态笔数 / 近 7 日报修量
- 视觉规范：深色侧栏 `#111827→#1f2937` + 科技蓝 `#1d6fe0`（沿用）+ 页面 `#f2f4f7` + 内容容器白底圆角 12px + 卡片阴影微悬浮 + **内联 SVG 图标与纯 CSS 图表（不引 CDN/第三方库，医院内网必须能跑）** + 响应式（≥1200 展开 / 768–1200 折叠为 64px / <768 抽屉）
- 文件清单（13 新增 + 3 修改）：
  - 后端新增：`dto/DashboardStats.java`、`service/StatsService.java`、`controller/StatsController.java`
  - 前端新增：`css/admin-layout.css`；`js/admin/{api,store,router,layout,ui,app}.js`；`js/admin/views/{dashboard,reports-pending,work-orders,reports-history,reimbursements,departments,users,system}.js`
  - 修改：`static/admin.html`（470 行平铺 → 薄壳）、`static/service-worker.js`（CACHE → itsd-v3 + APP_SHELL 补齐）、`.rdoc_inspect/test-frontend.js`
  - **明确不动**：report/create/today/detail/login.html、`department-cascade.js|css`、现有 Controller/Service/Mapper、数据库结构
- 批次计划：①后端统计接口 ②前端基座+admin.html 壳+空视图 ③8 个视图迁移 ④SW 升版+全量回归+记忆落盘（每批结束汇报，等用户验证）
- 遗留待办（本方案外）：「token 解析在 6 个 controller 中重复」建议后续抽公共组件（本轮不做，避免混淆重构范围）

### D-007 | 2026-10-14：后台前端架构决策（无构建模块化 / 占位策略 / jsdom 测试）

- 状态：accepted
- 来源：模型推断（落实 D-006 时的实现决策），总体方案已获用户确认
- 决策与理由：
  1. **无构建的模块化**：用 `window.Admin` 命名空间 + 脚本按序加载（`api → store → ui → router → layout → views/* → app`），**不引入 webpack/vite/框架**。理由：项目是 Spring Boot 静态资源直出，保持「无构建步骤、改完刷新即生效」，与既有技术选型（原生 H5、不引 CDN）一致
  2. **文件职责单一**：`api.js` 统一 fetch（X-Token/超时/错误归一化）、`store.js` 状态与缓存（科室/用户缓存 + 订阅）、`ui.js` 原子组件、`router.js` 路由、`layout.js` 菜单树与骨架、`views/*.js` 一视图一文件、`app.js` 引导。**新增视图 = 新建 `views/x.js` + `admin.html` 加一行**，无需改动其它文件
  3. **XSS 防护改进**：`ui.h()` 用 `textContent` 渲染文本；只有列渲染显式返回字符串时才按可信 HTML 处理（内部拼接用）
  4. **批次 2 占位策略**：未迁移的 7 个菜单用 `ui.placeholderRoute()` 占位，避免点击出现空白/报错；批次 3 逐个替换为真实视图
  5. **零第三方依赖**：图标为内联 SVG、图表为纯 CSS（`.trend` / `.dist`），医院内网与离线可用
  6. **权限语义优化**：401 → 清登录态并跳登录页；**403 → 只提示不登出**（旧版对 401/403 一律登出，属于过度反应）
  7. **Service Worker 提前升版**：批次 2 就 `itsd-v2 → itsd-v3` 并把 admin 全部静态资源纳入 `APP_SHELL`（新增 15 个文件且 SW 是 cache-first，不升版会一直吃旧文件）→ 批次 3/4 改动静态资源时需**再次升版**
  8. **测试方式升级**：`jsdom` 安装到 `.rdoc_inspect`（已 gitignore），`test-admin-app.js` 用真实 DOM + **真实后端接口**做集成测试（比手写 DOM 桩更接近浏览器）
- 规模对照：`admin.html` 479 行 → **45 行薄壳**；逻辑拆分到 6 个基座模块（api 70 / store 88 / ui 367 / router 100 / layout 225 / app 49 行）+ 8 个视图（dashboard 201 行，其余 7 个为 12 行占位）
- 目录约定：`static/js/admin/**`、`static/css/admin-layout.css`

### D-008 | 2026-10-14：后台重构批次 3 实现要点（视图迁移 + 系统信息接口）

- 状态：accepted
- 来源：模型推断（实现决策；方案见 D-006、架构见 D-007）
- 关键决策：
  1. **列表视图统一用 `ui.listSection()`**：把「卡片 + 工具栏搜索 + 表格 + 分页 + 计数」封成一个可复用区块，7 个视图不再各写一遍（内部维护 keyword/page，`controls` 插槽放状态下拉等筛选项）
  2. **测试不改动演示数据**：派单只「打开弹窗 + 校验可选项 + 取消」，不确认提交；生成报销单因同科室同月幂等而安全 —— 保证回归测试可反复跑不污染数据
  3. **详情抽屉按区块渲染**（基本信息/报修信息/流转记录/维修记录/耗材更换记录/签字状态），耗材行内直接核价（`input[data-id]` → `POST /consumables/pricing`）后回调刷新列表
  4. **新增 `GET /api/system/info`**（`SystemInfoController`）：返回应用/版本/端口/Java/OS/启动时间/运行时长/上传目录/数据库地址与账号/数据规模；**不读取也不返回数据库密码**；仅 ADMIN/DISPATCHER
  5. **「已知限制」在系统信息页对使用方透明展示**（内容与 `D-003` 对齐：报销单签名未落库、维修端状态映射、防重口径、工单详情越权、密码明文）
  6. 报销单页把「列表合计/待签字/待报销/已报销」做成 4 张统计卡，替代旧版一次性表格
- 视图规模：dashboard 201 / work-orders 205 / reimbursements 197 / system 158 / reports-pending 140 / departments 108 / users 87 / reports-history 82 行
- 缓存：批次 3 修改了 7 个视图 JS → `CACHE itsd-v3 → itsd-v4`（APP_SHELL 无新增文件）
- 遗留：批次 4 做全量回归 + 记忆落盘；`findings.md#D-003` 的 5 条已知限制仍未修（已在系统信息页披露，是否修由用户决定）

### D-009 | 2026-10-14：修复 D-003 的 5 条已知限制（用户决定「修」）

- 状态：accepted（用户明确要求）
- 来源：用户明确要求
- 用户原话（逐字）：> "已知限制是否修：修。之后做批次 4：全量回归 + 收尾（含你上面定的"已知限制是否修"）。"
- 逐项修复方案：
  1. **报销单签字留痕**：`reimbursement` 新增 `signature_url`；`ReimbursementService.sign()` 落库；管理员报销详情抽屉新增「签字留痕」分区、科室端报销明细展示签名图
  2. **维修端状态映射**：`today.html` / `detail.html` 的 `WORK_ORDER_STATUS` 补齐 `PENDING_ACCEPT:'待验收'`（不再出现英文枚举名）
  3. **防重口径**：`ReportService.findDuplicate()` 由「科室+设备名」改为 **「科室+设备名+报修人+问题描述」在 3 分钟内完全一致** 才算重复（窗口常量 `DUPLICATE_MINUTES=3`，文案说明可改设备/描述继续提交）；写库时把空的 problemDesc 归一为 `""` 以便比对；科室端倒计时由 10 分钟缩短为 **1 分钟**（仅防手抖），并给出「如需报修其他设备请等倒计时结束」提示
  4. **工单详情越权**：`WorkOrderController.detail()` 新增 `canView()` 按角色校验——管理员/调度员看全部、维修工仅 `assigneeId` 本人、报修人仅自己创建的工单或自己报修单派单生成的工单（需 `ReportMapper` 关联查询）
  5. **密码 BCrypt**：引入 `spring-security-crypto`（Boot 父 POM 管理 5.7.11，**仅用 BCryptPasswordEncoder，不引入完整 Security 过滤链**）；`AuthController` 用 `matches()` 校验，**历史明文命中后自动升级为哈希**；`data.sql` 种子密码改为哈希（$2a$10$…）
- 迁移：新增 `sql/2026-10-14-signature-and-bcrypt.sql`（幂等：缺列才 ALTER、密码仍是明文才 UPDATE），并已在当前数据库执行
- 系统信息页同步维护：原「已知限制」5 条转为「近期修复（2026-10-14）」，并重新列出**仍存在**的 6 条限制（浏览器端分页、科室端弱凭证、无后端单测、PWA 需 HTTPS、无待办提醒、无导出）
- 验证：见 `progress.md#S-008`（24 项接口端到端 + 前端断言全通过）

### D-010 | 2026-10-15：签字体验改造（签名板重写 + 输入姓名生成 + 签字方式留痕）

- 状态：accepted（用户明确要求；AI 先诊断出 3 个缺陷再给方案，用户选定范围）
- 来源：用户明确要求 + 模型推断（缺陷诊断）
- 用户原话（逐字）：> "我觉得科室签字那部分做的不太好，用鼠标操作在那个框里面不好操作，但是医院工作机，只能偶这样了，并且我在电脑上用鼠标模拟签字确实不太好演示，你有好的构建这一块的方案吗？"
- 用户选定方案（逐字选项）：**方案 1**「修好画布 + 鼠标手感（撤销/平滑/引导/自动裁剪）」+ **方案 2**「新增『输入姓名生成签名』便捷模式（默认仍为手写）」；**不加**「上传签字照片」与「二维码转手机签字」；**加 `sign_type` 字段并在界面展示签字方式**；范围 = **科室端与后台同步一致**
- **诊断出的 3 个真实缺陷（改造前代码，证据在旧 `report.html`）**：
  1. **画布像素与显示尺寸不一致**：`<canvas width="780" height="260">` 但 CSS `width:100%;height:140px`，而绘制用 `e.offsetX/offsetY`（CSS 像素 0~400×0~140）→ **笔迹只落在画布左上角约 40%×54% 区域**，导出 `toBlob` 又是整张 780×260，**存下来的签名又小又偏**（这是"难操作"的主因，与鼠标无关）
  2. **未设线宽/笔头**：无 `lineWidth`（默认 1px）、无 `lineCap/lineJoin` → 鼠标写出来像头发丝、带尖角毛刺
  3. **无撤销、无引导、无 DPR 适配**：画错只能全部清除；高分屏模糊；用户不知道在哪写、写多大
- 改造方案（落地）：
  1. **新建共用组件** `static/js/signature-pad.js` + `static/css/signature-pad.css`：按「显示尺寸 × devicePixelRatio」设置画布像素并用 `setTransform` 对齐 CSS 像素（坐标直接用 `clientX-rect.left`）；`lineWidth 3` + 圆头圆角；**中点二次贝塞尔平滑** + `getCoalescedEvents()` 合并高密度点；**逐笔撤销**（笔画数组重放，resize 后也能正确重绘）；引导文字/基准线画在 **CSS 层**（不进导出图）；**导出前自动裁剪墨迹包围盒 + 14px 留白**
  2. **双模式**：`HANDWRITTEN`（默认，医院正式流程）/ `TYPED`（输入姓名用楷体斜体渲染，字号自适应）——同一画布、同一上传链路
  3. **签字方式留痕**：新增 `model/SignType` 枚举（含 `of()` 容错解析，脏值回落 HANDWRITTEN）；`work_order.sign_type`、`reimbursement.sign_type`；`SignRequest`/`ReimbursementSignRequest` 增加 `signType`；科室端提交带 `signType`，后台两处详情展示徽标（`TYPED` 用 warn 色提示是便捷模式，历史 NULL 显示「未记录」）
- 迁移：新增 `sql/2026-10-15-sign-type.sql`（幂等 ALTER，已在当前库执行）
- **顺带产出**：项目**首个后端单元测试** `src/test/java/.../SignTypeTest.java`（6 项，JUnit 5），系统信息页的「无后端单测」限制同步改写
- 缓存：新增 2 个静态资源 → `CACHE itsd-v5 → itsd-v6` 且纳入 `APP_SHELL`
- 验证：见 `progress.md#S-014`（组件专项 22 项 + 端到端 11 项 + 三套回归 + JUnit 6 项全通过）
- 说明：**「上传签字照片」与「二维码转手机签字」两种方案本次未做**——前者最贴医院纸质流程、后者是跨设备亮点，都记录为后续可选（若做，`SignType` 需补 `PHOTO`）

### D-011 | 2026-10-15：权限体系落地（参考《设计重构.md》筛选后的 A 档）

- 状态：accepted（用户明确要求「A 档全做」，并强调「不要过度设计」「挂起/驳回/取消 后续版本再加」）
- 来源：用户提供参考文档 + 模型判断（逐条筛选采纳/不做）
- 用户原话（逐字）：> "根据你的理解和想法，找到更具落地的执行方案，但是切记不要过度设计，设计重构.md只是参考，其中那个工单"已驳回/已取消/已挂起"可以先不用实现，后续进行版本更新时加上，切记切记，不要过度设计。" → 之后：> "A 档全做"
- **参考文档中「已经满足」的 4 项**（无需改动，写进文档避免重复讨论）：
  1. **财务剥离**：`work_order` 里没有「待报销」状态；`reimbursement` 是独立实体 + 独立状态机（待签字→待报销→已报销）
  2. **行级权限**：维修工仅本人 `assigneeId`、科室仅本科室 `departmentId`、管理员/调度员全量（`D-009` 已实现）
  3. **状态机动态权限雏形**：每个动作都校验前置状态（接单要求待接单、提交维修要求维修中、签字要求待签字）
  4. **前端按状态渲染操作**：维修端只在对应状态显示表单、后台只在待报销显示财务回填
- **A 档落地内容（本次实现）**：
  1. **A1 权限单一事实来源**：新增 `service/WorkOrderAccess`（行级 `canView` + 状态机动态 `allowedActions`，动作常量 `VIEW/ACCEPT/REPAIR/PRICE/SIGN`）；`WorkOrderController` 的 detail/consumables/accept/repair/sign/pricing **全部改走该类**（删掉控制器里的私有 `canView` 与散落的角色判断）；`GET /api/work-orders/{id}` 改为返回 `WorkOrderDetail{workOrder, allowedActions, reportNo, reimbursement}`
  2. **A2 新增 FINANCE 角色**：`UserRole.FINANCE("财务")` + 演示账号 `finance/123456`（id=6）；`ReimbursementController` 的列表/详情/生成/回填放行财务（`isReimbursementStaff()` 一处判定）；后台菜单**按角色渲染**（`layout.visibleMenu()`），财务只见「财务与报销」，`defaultPath()` 财务落地 `/reimbursements`（`router.fallbackPath` 同步）；`admin.html` 入口鉴权由 `isAdminLike` 放宽为 `isBackendUser`，`refreshBadges` 对无统计权限的角色直接跳过
  3. **A3 文案统一 + 默认过滤**：`PENDING待受理 / ASSIGNED待接单 / IN_PROGRESS维修中 / PENDING_ACCEPT待签字 / COMPLETED已完工`（后端枚举 label + ui.js `STATUS.WORK_ORDER` + today/detail/report 三端映射共 5 处一致）；工单管理页默认范围 **「进行中」（=待接单+维修中+待签字）**，下拉可切「全部状态/单状态」，浏览器端过滤（服务端分页仍是已知待优化项）
  4. **A4 「报修单→工单」关系在 UI 显式化（替代单表合并）**：详情返回服务端计算的 **`reportNo`**（不再前端拼 `R000001`）；待受理页副标题写明「派单后生成工单并进入维修人员今日任务」
  5. **A5 报销进度**：`WorkOrderDetail.reimbursement{id, month, totalAmount, status}`（按 `reimbursement_item` 反查，`WorkOrderService.buildDetail()` 组装）；工单详情抽屉新增「报销进度」行
- **明确不做（C 档，附理由，避免以后反复讨论）**：
  1. **单表合并报修单/工单**：收益是概念整洁，代价是动数据模型 + 实体 + 4 个控制器 + 5 个页面 + 全部测试 + 存量数据迁移；UI 显式化已拿到 80% 收益
  2. **已挂起 / 已驳回 / 已取消**：用户点名「后续版本再加」
  3. **状态流水表**：其主要价值是记录异常流转与原因（挂起/驳回），与三态一起做才划算；当前 5 个正常态用 `signedBy/signedAt/completedAt` 已够追溯
  4. **冷热数据分离 + N 天自动归档**：当前「2 名维修工 + 百级工单」量级无必要（十万级才谈分区/归档）
  5. **RBAC 配置表（角色-权限-菜单关系表）**：角色固定 5 个、规则稳定，枚举 + 代码内矩阵足够；上表意味着还要配套管理界面
  6. **完整字段级脱敏框架**：当前无成本核算/绩效/内部备注等敏感字段，属预置过度设计
  7. **WebSocket / STOMP / 消息队列**：实时性需求（派单后维修工需刷新）用更省的方式即可，SSE 也只列为后续可选
- **权限矩阵（横轴角色 / 纵轴操作）**：

  | 操作 | 报修人 | 科室端(免登录) | 维修工 | 调度员 | 管理员 | 财务 |
  |---|---|---|---|---|---|---|
  | 提交报修 | ✅ | ✅ | — | — | — | — |
  | 看待受理池 / 派单 | — | — | — | ✅ | ✅ | — |
  | 接单 / 提交维修 / 耗材登记 | — | — | ✅仅本人 | — | — | — |
  | 耗材核价 | — | — | — | ✅ | ✅ | — |
  | 工单签字 / 报销单签字 | — | ✅本科室 | — | — | — | — |
  | 生成月度报销单 | — | — | — | ✅ | ✅ | ✅ |
  | 查看报销单 | — | ✅本科室 | — | ✅ | ✅ | ✅全量 |
  | 财务回填 | — | — | — | — | ✅ | ✅ |
  | 工单列表 / 工单详情 / 今日任务 / 报修单池 | — | — | 仅本人今日任务 | ✅ | ✅ | ❌ |
  | 用户管理 / 系统信息 / 概览统计 | — | — | — | ✅ | ✅ | ❌ |
  | 科室索引 | — | — | — | ✅ | ✅ | ❌ |
- 动态权限示例（本次单测固化）：维修工在「待接单」→`[VIEW,ACCEPT]`、「维修中」→`[VIEW,REPAIR]`、「待签字」→`[VIEW]`（收窄为只读）；科室端在「待签字」→`[VIEW,SIGN]`，其它状态仅 `[VIEW]`
- 迁移：新增 `sql/2026-10-15-finance-role.sql`（幂等 INSERT IGNORE，已执行）
- 验证：见 `progress.md#S-015`（后端单测 17 项 + 财务权限 16 项 + 签字/动态权限 15 项 + 三套 UI 回归 + 批次 4 接口 24 项全通过）

### D-012 | 2026-10-15：跨端状态同步缺口（**未决 / 待诊断**，最高优先级）

- 状态：**open（待用户确认复现场景）**
- 来源：用户复验 A 档后的直接反馈
- **用户原话（逐字）**：> "完全错了，状态还是没有同步上。可能是是触发了上下文压缩。这样，你先保留项目进度，我切换对话进行项目推进。"
- **已核实的事实（代码层面，可直接引用，无需重查）**：
  1. 全仓库**没有任何状态推送或自动刷新机制**：`setInterval` 仅存在于 `report.html:381`（1 分钟防重倒计时）；**不存在** `EventSource`(SSE) / `WebSocket` / `visibilitychange` 监听
  2. 维修端 `today.html`：只在页面加载时拉 `/api/work-orders/today` → 管理员派单后，维修工**不手动刷新就看不到新任务**
  3. 科室端 `report.html`：`loadAccept()` / `loadCompleted()` 只在页面加载与自身操作回调中调用 → 维修工提交维修后，科室端**不刷新看不到「待签字」**
  4. 后台 `admin.html`：视图切换时才重新拉数据；`Admin.refreshBadges()` 仅在 boot 与工单页 paint 后触发 → 科室签字后，**后台角标/列表不会自更新**
  5. 综合：**任何一端的操作，其它两端都不会自动看到**
- **根因（AI 取舍失误，应向用户承认）**：参考文档《设计重构.md》「实时同步机制」章节明确要求「使用 WebSocket 或 SSE 推送，而非前端轮询」；AI 在 A 档筛选时把「WebSocket / 消息队列」列入**不做**、把 SSE 与「页面重新获得焦点自动刷新」列入 **B 档可选**，而用户批准的只有 A 档 → **同步这件事一行代码都没做**。教训：被用户判定为"完全错了"的往往是**用户最直观能感受到的体验**（跨端看到同一个状态），而非权限矩阵这类结构性收益。
- **待确认的两种可能（下一会话先问用户，不要直接开写）**：
  1. **跨端同步缺口**（最可能）：期望「派单后维修端自动出现新任务 / 签字后后台自动更新」，实际必须手动刷新
  2. **某端状态文案或映射不一致**：A3 统一了 5 处映射（后端 `WorkOrderStatus` label、`ui.js#STATUS.WORK_ORDER`、`today.html`、`detail.html`、`report.html`）——若属此类，需要用户提供具体页面 + 截图后逐处比对
- **实现选项（按性价比，勿过度设计）**：
  - a. **便宜版（约 10 行）**：页面重新可见/获得焦点时自动重新拉取 + 维修端下拉刷新
  - b. **正式版（约 1 批次）**：Spring `SseEmitter` 单向推送（新报修 / 派单 / 状态变更），后台与维修端订阅 + 断线重连；**不做 WebSocket / STOMP / 消息队列**；部署时 Nginx 需对 SSE 关闭缓冲
- 关联：`progress.md#S-016`、`handoff.md`「未解决：跨端状态同步」

### D-013 | 2026-10-15：工单业务管理重构——报修单状态随工单流转同步 + 科室端历史列表整合

- 状态：accepted（用户明确要求重构工单业务管理，且明确「不要过度设计」）
- 来源：用户明确要求 + 模型诊断（实地核查代码 + 数据库现状）
- 用户原话（逐字）：> "继续推进it-service-desk项目，尤其是工单业务管理方面，设计的比较混乱，工单状态没有同步，并且历史工单当中工单的最后状态应该是已完成。针对这一块，结合你的开发经验，进行重构，但是切记不要过度设计。"
- **诊断结论（已核实，非推测）**：
  1. **报修单状态"永远停在已派单"（数据层根因）**：`ReportService.dispatch()` 派单后 `report.status = DISPATCHED`，**之后再无任何代码更新它**。实测 R000001/R000013 的 report.status 都是 DISPATCHED，但对应工单早已 COMPLETED+SIGNED → 科室端「报修历史」永远显示「已派单」，看不到「已完成」。**这就是用户说的「历史工单的最后状态应该是已完成」**
  2. **科室端 4 张分散卡片，设计混乱（展示层）**：report.html 原有 4 张并列卡片——待验收工单 / 已完成工单 / 月度报销单 / 本科室报修历史。其中"报修历史"(report表)与"已完成工单"(work_order表)讲的是同一批设备的不同生命周期阶段，用户得跨卡片对照才能拼出全貌
- **取舍（不做清单，防过度设计）**：
  - ❌ 报修单**不逐流转步回写**（不每步接单/维修中/待签字都更新 report）：report 只需表达「请求是否已了结」，工单细粒度流转在 WorkOrderStatus 维护；两者在「已完工」处汇合。逐步回写会与工单状态机重复耦合
  - ❌ 不做 SSE/WebSocket 实时推送：本次诉求是工单业务管理的状态同步与历史展示，不是跨端实时推送；实时性留 Day7/后续版本再定
  - ❌ 不加状态流水表、不合并 report/work_order 单表、不加已挂起/已驳回/已取消（D-011 已论证）
- **改动（4 项最小改动，不新建表、不改数据模型）**：
  1. **A 数据层（~15 行）**：`ReportStatus` 枚举去掉从未使用的 `ACCEPTED`、新增 `COMPLETED("已完成")`；`WorkOrderService.sign()` 签字完工时回写 `report.status = COMPLETED`（reportMapper 已注入）。状态机变为 `PENDING(待受理) → DISPATCHED(已派单/处理中) → COMPLETED(已完成)`
  2. **B 科室端历史整合（~40 行）**：report.html 删掉独立的「已完成工单」卡片，将其信息并入「本科室报修与维修历史」单一列表——报修单为主轴，`loadHistory()` 并发拉 report + work_order，按 reportId 建索引，已完成报修单附带工单号+签字人+完成时间+查看详情；签字成功后刷新历史列表（原代码漏调 `loadHistory()`）
  3. **C 后台状态映射（~5 行）**：`ui.js#STATUS.REPORT` 去 ACCEPTED 加 COMPLETED(success色)；`reports-history.js` 下拉选项去 ACCEPTED 加 COMPLETED + 副标题更新；`system.js` 报修单状态机图补 `→ COMPLETED` 终态
  4. **D 迁移脚本**：`sql/2026-10-15-report-status-sync.sql` 幂等回填存量（工单 COMPLETED 的 report 从 DISPATCHED 更新为 COMPLETED；ACCEPTED 兜底转 DISPATCHED），已在当前库执行
- 落地文件：
  - 改：`model/ReportStatus.java`、`service/WorkOrderService.java`、`static/report.html`、`static/js/admin/ui.js`、`static/js/admin/views/reports-history.js`、`static/js/admin/views/system.js`、`static/service-worker.js`(itsd-v7→v8)
  - 新增：`sql/2026-10-15-report-status-sync.sql`
- 验证证据（8090 临时实例实跑闭环 + 三套前端回归 + JUnit）：
  - 闭环：报修(R000014 PENDING)→派单(report=DISPATCHED/wo=ASSIGNED)→接单(IN_PROGRESS)→维修(PENDING_ACCEPT)→签字(wo=COMPLETED+SIGNED)→**查库 report.status=COMPLETED ✓**（这是核心验证点）
  - 接口：科室端 `/api/reports/by-department` 返回 status=COMPLETED；后台 `/api/reports` 全部 COMPLETED；`/api/work-orders/by-department` 带工单号+签字人
  - `mvn -Dmaven.repo.local=.m2repo test`：17 项全过（SignType 6 + WorkOrderAccess 11）
  - 前端 jsdom：`test-frontend.js`(report/create/admin 语法+逻辑)、`test-admin-app.js`(8 大区块)、`test-sign-type.js`、`test-finance-api.js` 全部 ALL CHECKS PASSED
  - 测试造数据已按 id>13(id>7) 清理，基线恢复：report 2(R000001/R000013)、work_order 2(#1/#7)、consumable 1、reimbursement 1
- 关联：解决 `D-012` 中用户反馈的「历史工单的最后状态应该是已完成」这一具体诉求；`D-012` 的「跨端实时推送」部分仍未做（不在本次范围）→ `progress.md#S-017`

### D-014 | 2026-10-15：报销闭环同步——财务回填后报修单回写「已报销」

- 状态：accepted（用户明确要求，延续 D-013 的最小改动原则）
- 来源：用户明确要求
- 用户原话（逐字）：> "下面进行报销单的部分，根据保修单历史进行处理，之后报修单历史当中状态为"已报销"状态"
- 决策：`ReportStatus` 新增 `REIMBURSED("已报销")`；`ReimbursementService.reimburse()` 财务回填成功后按报销单明细反查工单 → 回写对应 `report.status = REIMBURSED`（私有方法 `syncReportsReimbursed`，**只推进 COMPLETED 状态的报修单**，幂等防御旧数据/重复回填）。报修单状态机最终定为：`PENDING → DISPATCHED → COMPLETED → REIMBURSED`（+CANCELLED 终态）
- 前端同步：`ui.js#STATUS.REPORT`、`reports-history.js` 下拉/副标题、`system.js` 状态机图、`report.html` 映射与历史列表展示（已报销用紫色 #8e44ad 区分于绿色的已完成）
- 迁移：`sql/2026-10-15-report-reimbursed-sync.sql` 幂等回填存量（已报销报销单内工单对应的 COMPLETED 报修单 → REIMBURSED），已在当前库执行
- SW：itsd-v8 → itsd-v9
- 验证证据（8090 实例端到端实跑）：生成报销单#9（含工单#7→R000013）→ 科室签字 → 财务回填 → 查库 R000013/R000016 均 REIMBURSED ✓；`mvn test` 通过；test-frontend/test-admin-app/test-finance-api 全部 ALL CHECKS PASSED；测试数据已清理，基线恢复（R000001=已报销 对应报销单#1，R000013=已完成，报销单 1 张）
- 关联：`findings.md#D-013`、`progress.md#S-018`

### D-015 | 2026-10-15：工单状态机重构——严格对齐《工单全流程规则.md》6 态

- 状态：accepted（用户提供规则文件，明确要求严格按规则设计；4 个关键决策点经 ask_user_question 确认：6态/核价独立/签字→待报销移入历史/报修单粗粒度同步）
- 来源：用户明确要求 + 规则文件《工单全流程规则.md》
- 用户原话（逐字）：> "严格按照上述流程进行设计，先给出你的设计方案。"
- **规则与现状的差距（已核实代码）**：
  - 规则要求 6 态：待接单/维修中/待核价/待签字/待报销/已报销；旧代码 5 态：待受理(未用)/待接单/维修中/待签字/已完工——缺待核价/待报销，已完工≠已报销
  - 规则步骤4：维修提交关闭→待核价，管理员核价→待签字；旧代码核价在详情随时改，与状态机脱钩
  - 规则步骤5：签字→待报销（移入历史）；旧代码签字→已完工
- **状态机重构**：
  - `WorkOrderStatus` 严格 6 态：`ASSIGNED(待接单)/IN_PROGRESS(维修中)/PENDING_PRICING(待核价)/PENDING_SIGN(待签字)/PENDING_REIMBURSE(待报销)/REIMBURSED(已报销)`，去掉 PENDING/COMPLETED
  - 流转：派单→待接单→接单→维修中→提交关闭→待核价→核价→待签字→签字→待报销(移入历史)→月度报销→已报销
  - 核价提升为状态机正式一环：`pricing` 接口从"随时改价"变为"核价完成推进状态 PENDING_PRICING→PENDING_SIGN"
  - 签字后→PENDING_REIMBURSE（待报销，移出工单管理）；月度报销→REIMBURSED
- **报修单粗粒度同步**（保持，不逐态回写）：
  - 工单 PENDING_REIMBURSE（已签字验收）时 report→COMPLETED（已完成）
  - 工单 REIMBURSED（已报销）时 report→REIMBURSED（已报销）
  - 工单的待核价/待签字中间态不回写报修单（避免耦合）
- **页面可见性**（方案A，工单管理状态下拉覆盖，不新建视图）：
  - 工单管理默认"进行中"= 待接单+维修中+待核价+待签字（4态）；下拉可切"待报销/已报销"看历史
  - 即"待报销/已报销移出工单管理"的语义 = 默认不显示，切下拉才看
- 落地文件：
  - 后端：`WorkOrderStatus.java`(6态)、`WorkOrderService.java`(submitRepair→待核价/completePricing→待签字/sign→待报销/reimburse→已报销+回写report)、`WorkOrderAccess.java`(PRICE在待核价/SIGN在待签字/新增REIMBURSE在待报销)、`WorkOrderController.java`(pricing语义变更+新增/{id}/reimburse)、`ReimbursementService.java`(reimburse回填时工单→已报销+注入WorkOrderService)、`StatsService.java`+`DashboardStats.java`(指标口径用新枚举)、`SystemInfoController.java`
  - 前端：`ui.js`(STATUS.WORK_ORDER 6态)、`work-orders.js`(ACTIVE 4态/下拉8选项/核价按钮待核价才显示/新增报销按钮)、`system.js`(状态机图)、`dashboard.js`(待签字字段)、`app.js`(activeWork)、`today.html`/`detail.html`/`report.html`(WO_STATUS映射)、`reports-history.js`
  - 迁移：`sql/2026-10-15-workorder-status-machine.sql`(幂等映射存量：PENDING_ACCEPT→PENDING_SIGN、COMPLETED+SIGNED+已报销→REIMBURSED、COMPLETED+SIGNED+未报销→PENDING_REIMBURSE、COMPLETED+UNSIGNED→PENDING_PRICING)，已执行
  - 单测：`WorkOrderAccessTest.java`(12项，新增 REIMBURSE 权限测试)
  - SW itsd-v9 → itsd-v10
- 验证证据（8090 实跑闭环 7 步全绿）：
  - 报修(PENDING)→派单(工单ASSIGNED/报修DISPATCHED)→接单(IN_PROGRESS)→维修提交(PENDING_PRICING)→核价(PENDING_SIGN)→签字(PENDING_REIMBURSE/报修COMPLETED)→月度报销(工单REIMBURSED/报修REIMBURSED) ✓
  - mvn test 通过（WorkOrderAccess 12 项）
  - 前端 test-frontend/test-admin-app/test-finance-api/test-sign-type 全部 ALL CHECKS PASSED（测试断言已同步新状态机）
  - 存量迁移：工单#1→REIMBURSED、工单#7/#11/#12→PENDING_REIMBURSE
- 关联：`findings.md#D-013`、`#D-014`、`progress.md#S-019`

### D-016 | 2026-10-15：科室索引管理修复——搜索失焦 + 布局重排

- 状态：accepted（用户明确要求，附截图指出两处问题）
- 来源：用户明确要求
- 用户原话（逐字）：> "科室索引管理板块：1.右边搜索功能有问题，赶紧修改 2.左边科室索引部分全为竖着一列并不好看，按照你的审美和理解，要让对科室信息管理人员一目了然，看着舒服。"
- **诊断出的两个真实根因（已核实代码）**：
  1. **搜索打一个字就失焦**：`onInput → paint(el)` 每次输入把整张卡片**连搜索框一起销毁重建** → DOM 引用变化、焦点丢失，无法连续输入
  2. **竖着一列的原因**：departments.js 用的 `.dept-campus` / `.dept-building` / `.dept-floor-row` / `.dept-names` / `.dept-name-chip` 五个类在 admin-layout.css 里**从未定义**（floor-chip 在 dept-cascade.css，但 dept-* 系列哪儿都没有）→ 全部 div 块级堆叠成竖列
- **修复**：
  1. **搜索框只创建一次**：工具栏卡片在 render 时固定创建，输入只重绘 `#dept-content` 数据区（搜索框 DOM 引用不变、焦点保留）；卡片头计数 hint 手动同步；`departments.js` 裸引用 `DeptCascade` 改为 `window.DeptCascade`（更健壮）
  2. **布局重排**：院区三栏卡片 grid（`repeat(auto-fit, minmax(320px,1fr))`）→ 楼栋分区（蓝色左边框标题 + 科室数）→ 楼层行（蓝色 floor-chip + 科室 chips 横向流式 flex-wrap、白底胶囊、hover 变蓝、超长省略号）；窄屏 ≤1100px 自动降单列
- 新增专项测试：`.rdoc_inspect/test-departments-view.js`（15 项：视图注册/grid 结构/3 院区/6 楼栋/36 楼层行/68 chips/计数 hint/搜索不失焦/值保留/过滤 22/清空恢复 68/负责人搜索逻辑）
- **附带发现**：department 表 `department_head` 字段**全部为空**（68/68）→ 负责人搜索逻辑正确但无数据可搜；悬停提示也不会显示负责人。若需要按负责人搜索/展示，需先维护科室负责人数据（可后续做基础数据编辑功能）
- 测试断言同步：test-frontend.js 的 today/detail 状态映射断言从旧 5 态（PENDING_ACCEPT/COMPLETED）更新为 D-015 的 6 态（PENDING_PRICING/PENDING_SIGN/PENDING_REIMBURSE/REIMBURSED）——这是上轮状态机重构的测试遗留，本轮一并修
- SW：itsd-v10 → itsd-v11
- 验证：专项 15 项 ALL PASSED + test-frontend / test-admin-app 全部 ALL CHECKS PASSED
- 关联：`progress.md#S-020`

### D-017 | 2026-10-15：科室索引搜索功能移除（用户裁定冗余）

- 状态：accepted（用户明确要求）
- 来源：用户明确要求
- 用户原话（逐字）：> "将该搜索功能删掉，我觉得有些冗余了，下面的列表信息就一目了然了，不需要这块的功能。"
- 决策：科室索引页移除搜索框与过滤逻辑（filteredGroups/state.keyword/「当前筛选结果」指标卡改为「当前展示」），只保留「刷新」按钮。理由：院区→楼栋→楼层三级分类布局本身已一目了然，68 个科室规模下搜索无必要
- 落地：重写 `js/admin/views/departments.js`（142→105 行）；专项测试改为断言「无搜索框 + 刷新可用」；test-admin-app 的「科室页有搜索框」断言同步反转；SW itsd-v11 → itsd-v12
- 验证：专项 9 项 ALL PASSED + test-admin-app ALL PASSED
- 关联：`progress.md#S-021`

### D-018 | 2026-10-15：签字弹窗费用明细——核价后费用对签字人透明

- 状态：accepted（用户明确要求）
- 来源：用户明确要求
- 用户原话（逐字）：> "科室报修页面，工单签字验收环节下面应该加上管理员核价之后的报修单费用，科室，外包公司耗材管理员，医院财务都知道每次维修费用，价格对环节所有人都透明，避免扯皮。"
- 决策：科室端签字验收弹窗（report.html signModal）在「维修记录」下方新增**费用明细区**：耗材名 × 数量 × 核价单价 + 小计 + 合计金额。数据源 = 科室端耗材接口 `/api/work-orders/{id}/consumables?departmentId=`（签字发生在核价后，单价已填好）。**未核价项标红提示「请先联系管理员核价」并计 ¥0**；无耗材时费用区隐藏。护士长签字 = 对维修结果与费用同时确认（科室/管理员/财务四方费用透明）
- 落地：
  - `report.html`：signModal 加 `#signCost` 区块 + `renderSignCost()`（openSign 并行拉详情+耗材后渲染；resetSignPad 复位时隐藏；报销单签字模式不展示）+ `.cost-*` 样式
  - SW itsd-v12 → itsd-v13
- 新增专项测试：`.rdoc_inspect/test-sign-cost.js`（13 项：未核价标红/合计0、核价后小计与合计（80+15×2=110）、无耗材隐藏、真实后端数据流）
- 验证：专项 13 项 ALL PASSED（真实闭环造数→核价→科室端接口返回核价单价→renderSignCost 合计正确）
- 踩坑（测试脚本自身）：①helper 的 Object.assign 覆盖 headers 丢 Content-Type → repair 的 consumables 没写库（headers 须合并不能整体覆盖）②jsdom 无 canvas → stub HTMLCanvasElement.prototype.getContext
- 数据状态：用户全流程验证中（工单#1 待签字/碳粉¥40 已核价）；我的测试数据已清理
- 教训：**杀 java 进程必须按端口精确定位（netstat→pid），不能用启动时间过滤**——本次误杀了用户 IDEA 的 8080 实例，已提醒用户重启
- 关联：`progress.md#S-022`

### D-019 | 2026-10-15：UI 文案 + 详情费用 + 数据留存策略（按《数据留存策略.md》）

- 状态：accepted（用户提供需求规格文件，明确要求；方案先经确认）
- 来源：用户提供《数据留存策略.md》+ 两张截图（详情弹窗缺费用 / 历史标题文案）
- 用户原话（逐字）：> "这是Deep Seek的建议，之后根据上面两张粘贴的图片显示出的问题，并结合你对该项目具体情况的掌握，给出你的方案。" → 之后：> "先按上述文案开工"
- **需求规格与现状差距（已核查代码）**：
  1. 文案：report.html 标题「本科室报修与维修历史」→ 要求改「报修记录」
  2. 详情弹窗缺费用：openHistoryDetail 只显示耗材名"碳粉×1"，无单价无金额（图1问题）
  3. 报修记录无留存期：listReportsByDepartment/listReports 全量返回，无时间过滤
  4. 报销单无留存期：listAll/listByDepartment 全量返回
  5. 权限可见性（科室看本科室/管理员全量）已实现（D-011），无需改
- **改动（5 项，严格按需求，不做需求外的事）**：
  1. 文案：`report.html` 标题 →「报修记录」
  2. 详情费用：`openHistoryDetail` 在「耗材」行下加「报修费用」区——耗材×数量×核价单价+小计+合计金额，未核价标红，无耗材显示"无耗材费用"（复用 `.cost-*` 样式，纯展示层，后端零改动——耗材接口已返回 unitPrice）
  3. 报修留存：`ReportService` 加 `RETENTION_DAYS=60` 常量，listReportsByDepartment + listReports 加 `created_at >= now-60天` 过滤
  4. 报销留存：`ReimbursementService` 加 `RETENTION_DAYS=180`，listAll + listByDepartment 加 `created_at >= now-180天` 过滤
  5. 前端提示：报修记录卡片标题旁「（仅展示近 2 个月）」；reimbursements.js hint 加「仅展示近 6 个月」
- **取舍（不做清单，防过度设计）**：
  - ❌ 不加定时清理任务（@Scheduled）：需求规格说"定时清理**或**查询过滤"二选一；查询过滤更安全（数据不丢、可追溯、无需调度配置），符合"防误删"建议
  - ❌ 不加 is_deleted/archived_at 字段：查询时间过滤已满足"只展示近 N 个月"，无需改表结构
  - ❌ 不真存两份数据：需求规格"额外建议"已明示用 RBAC，项目已实现（D-011）
  - ❌ 不动签字弹窗费用（D-018 已做）；不动后台工单详情抽屉（耗材表格已有单价列）
- 落地文件：`report.html`、`ReportService.java`、`ReimbursementService.java`、`js/admin/views/reimbursements.js`、`service-worker.js`(v14)
- 验证证据（8090 实跑 + jsdom）：
  - 留存过滤：造 61 天前报修单 → 科室端/后台接口都不返回（库内仍 2 条，未物理删除）✓；今天创建的 R000001 正常返回 ✓
  - 费用明细：工单#1（碳粉¥40）→ openHistoryDetail 渲染「报修费用」「维修费用明细」「合计 ¥40.00」✓
  - 文案：标题「报修记录」+「仅展示近 2 个月」+ reimbursements「仅展示近 6 个月」✓
  - mvn test 通过；test-frontend + test-admin-app ALL PASSED
  - 新增专项 test-history-cost.js（10 项 ALL PASSED）
- 数据状态：用户全流程验证中（R000001 已完成/报销单#1 待报销）；测试造的 61 天前数据已清理
- 关联：`progress.md#S-023`

### D-020 | 2026-10-15：前端去 slop 重构（design-taste-frontend skill 走查）

- 状态：accepted（用户明确要求"AI 味太重、界面俗套"重构，三端同等力度）
- 来源：用户明确要求 + design-taste-frontend skill（tasteskill v2）
- 用户原话（逐字）：> "我的想法是将我的这个项目用该skill进行前端的重构，现在的前端AI味道太重了，并且界面也很俗套"
- **诊断（lint 55 文件 + inspect 三页截图）**：
  - 44 FAIL：28 resting 阴影 / 12 细线 / 16 描边盒 / 4 渐变 / 28 emoji / 1 对比度不足 / 4 100vh
  - 截图肉眼：所有页面同款"均值页"（居中窄卡+软阴影+灰底），桌面 1440 右侧大片空白（内容锁死 480px）
- **skill 匹配度判断（诚实记录）**：tasteskill 设计目标是落地页/营销页（hero 大图/scroll 叙事/4 张真实图 floor/live thing）。it-service-desk 是 B 端后台+移动工单，硬套落地页机制是错配。**取 skill 的高价值部分**（家规 no-slop + lint/inspect 量化 + type/colour/layout laws），**不取**落地页专属机制（idea sentence/found 真实图 floor/scroll motion——医院内网无外网图源且功能页不需要）
- **改造（三端同等力度，纯视觉层）**：
  1. **共享基座 admin-layout.css**：删全部 resting 阴影（28→0，浮层 drawer/modal/toast 保留 --shadow-overlay 并标 taste-skill-ignore）；删 hairline（topbar/menu-item/detail-row 改 tone-step inset 或空间分隔）；删 4 渐变（侧栏实色/hero 实色/趋势柱实色/骨架脉冲）；删描边盒（card/metric/input/btn-ghost 改底色 tone-step）；100vh→100dvh；字号对比拉大（page-head 18→22px，metric value 26→30px，正文 14→15px）
  2. **admin 视图层**：5 处 emoji 清零（🔒→禁/⚠️→误/✕→×/📭→空/✍⌨ 删）；router/ui/reimbursements/system.js
  3. **科室端 report.html**：card 去阴影；input/tag 去描边改底色；history-item 去虚线；cost-row 去虚线；签名图去 border；header 绿 #27ae60→#15803d（对比度 4.5:1+）；6 处 emoji 清零；#7f8c8d/#95a5a6→#6b7280（对比度修复）
  4. **create/today/detail/login**：同款 shadow/border/emoji 清理；字号拉大；login 演示账号框改 `<details>` 折叠（默认收起，安全+美观）；login 补 finance 账号
  5. **dept-cascade.css / signature-pad.css**：picker 浮层阴影合规标 ignore；picker-value/floor-chip 去描边；签名板基准线 + 输入框 border 改底色
  6. SW itsd-v14 → itsd-v15
- **验证**：
  - lint 全量：44 FAIL → **0 FAIL**（6 WARN 均合规：padStart 用于月份格式化、签名板基准线/输入框）
  - inspect report.html：5 FAIL（shadow/contrast/hairline/few-pictures/still-page）→ **对比度类 0 FAIL**；剩 few-pictures/still-page 2 项是落地页专属要求，对内网工具页不适用，报告声明豁免
  - mvn test 通过；前端 test-frontend/test-admin-app/test-departments-view/test-history-cost 全部 ALL PASSED
  - 踩坑：mvn clean 失败（jar 被占用）→ 改 mvn package（不 clean）+ 停 java 后等更久
- **不做清单（防过度设计）**：不引外网图片（医院内网）、不加 scroll 动效叙事、不做 hero 大图、不碰后端/业务逻辑
- 落地文件：admin-layout.css、dept-cascade.css、signature-pad.css、report.html、create.html、today.html、detail.html、login.html、js/admin/{router,ui}.js、views/{reimbursements,system}.js、service-worker.js
- 关联：`progress.md#S-024`
## D-021 | 2026-10-16：三端交付形态 = PWA 浏览器入口，不做 APK

**决策**：科室端/维修端做成 PWA（manifest + service worker + 安装引导），不做 APK。

**理由**：
1. APK 只覆盖安卓；维修工用个人手机，iOS 盲区无法回避，最终还是得维护 H5 兜底 = 两套入口
2. 内网 APK 无法上架，发版 = 全员手动重新下载安装，每次发版都付一次分发税；H5 刷新即生效
3. 个人手机装公司 APK 的权限申请有信任成本；H5 走浏览器沙箱
4. 业务缺口（后台推送提醒）在大陆内网恰恰不该用 APK 解决（无 GCM，厂商通道/极光个推才是真过度设计）——用企微 webhook 或短信补

**升级触发条件**（满足任一再考虑 Capacitor 包装）：真后台推送且无企微/短信可用；离线录入同步；传感器需求。包装时同一套 H5 代码产出 APK，无重写成本。

**三端账户隔离规则**（本次落地）：admin（管理员/调度/财务）/ repairer（today+detail）/ reporter（create）三端互不可达——角色不匹配时提示后强制退出回登录页，login.html 是唯一分发点；科室端 report.html 免登录工作机，退出 = 解绑科室。

## D-022 | 2026-10-16：报销职责分离——生成归运维，回填归财务

**问题**：管理员/调度员在「月度报销单」页能看到并点击「财务回填」，工单详情里还另有「确认月度报销」按钮；后端 isReimbursementStaff 把 ADMIN/DISPATCHER 也放进回填白名单 —— 越权且违背岗位职责。

**决策**：按岗位职责硬分离
| 动作 | 谁可执行 | 说明 |
|---|---|---|
| 生成报销单 | 仅管理员/调度员 | 按月汇总各科室已签字工单 |
| 查看列表/详情 | 管理员/调度员/财务 | 财务需要看单，运维需要看进度 |
| **财务回填** | **仅财务** | 资金动作，运维侧不得代填 |
| 科室签字 | 科室端免登录 | 不变 |

**落地**：
1. 后端 ReimbursementController 拆出 isOpsStaff（generate）与 isFinance（reimburse），list/detail 仍用 isReimbursementStaff
2. 移除工单级报销路径：删除 POST /api/work-orders/{id}/reimburse、WorkOrderAccess.REIMBURSE 常量与待报销动作规则；报销只能整张报销单回填，回填后由 ReimbursementService.syncWorkOrdersReimbursed 自动推进单内工单 → 再回写报修单（三端同步）
3. 前端 eimbursements.js 按角色渲染：运维侧显示月份选择+生成按钮、待报销行显示不可点的「待财务回填」提示；财务侧显示「财务回填」按钮、隐藏生成相关控件；副标题按角色给出不同说明
4. 前端 work-orders.js 移除详情抽屉里的「确认月度报销」按钮
5. 单元测试 WorkOrderAccessTest 同步：原「管理员待报销可报销」用例改为断言**不可报销**（仅 VIEW）

**验证**：后端接口角色矩阵（admin/dispatcher 回填=403、finance 生成=403、工单级报销=404）；jsdom UI 双角色断言 11/11；财务回填 + 跨端同步 15/15

## D-023 | 2026-10-16：浏览器返回键 = 退出登录（防重入）

**问题**：退出登录只清 localStorage 并跳 login.html，但已登录页面仍留在浏览器历史里；且浏览器前进/后退可能直接从 **bfcache（往返缓存）恢复页面快照、脚本不重跑** —— 用户按返回键就能"回到"已登录界面，看起来像没退出过，违背「三端只能退出后切换账户」。

**决策**：把返回键在语义上等同于退出登录，三层防护：
1. **退出用 location.replace**（guard.js + dmin/store.js + 科室端退出）：把当前已登录页面从历史记录中**替换**为登录页，返回键没有可回的已登录条目
2. **bfcache 恢复时重新校验**：pageshow 事件 persisted=true 表示页面来自往返缓存 → 重新读登录态，失效立即**静默**跳登录页（不弹窗，避免返回时被打扰）。共用逻辑收敛在 guard.js，dmin/app.js 另有一套（后台是 SPA）；三端均已覆盖
3. **服务端 HTML 禁止缓存**：WebConfig 拦截器对 *.html 与 / 下发 Cache-Control: no-store, no-cache, must-revalidate + Pragma: no-cache + Expires: 0，浏览器无法复用已登录页面快照（Chrome 下 no-store 还会直接禁用 bfcache）

**区分两种"未登录"体验**：完全无凭证 → 静默回登录页（返回键场景常见）；有凭证但角色不符 → 弹提示后强制退出（越权场景需要告知）。

**未采用**：把"登录状态下按返回键"也强制登出（history sentinel + popstate 拦截）。原因是后台是 hash 路由 SPA，应用内前进/后退会触发同样的事件，拦截会导致用户无法在页面间后退 —— 误伤大于收益。当前方案保证的是「退出后无法用返回键重入」，这是安全诉求的核心。

## D-024 | 2026-10-16：报修单与工单的清单归属——后台不重复展示报修单

**问题**：用户先后两次要求精简——先要求把「待受理报修单」「报修单历史」并入工单管理页的下拉，再要求把下拉里的报修单项也删掉。如果照做到底，后台将**没有任何派单入口**（报修单无法转工单，核心闭环断裂）。

**决策**：确立“报修单/工单的清单归属原则”，作为后续菜单与页面设计的硬约束：

- **报修单**是科室端 + 待受理队列的概念。后台只在「待受理报修单」页展示它（作为派单入口），**不进入工单管理页、不作为下拉项**。
- **工单**是后台 + 维修端的概念。工单管理页只列工单，状态过滤（全部/待接单/维修中/待核价/待签字/待报销/已报销）也只针对工单。
- 两份清单不交叉展示，避免“在工单页里又看到报修单”这类语义重复。
- 已流转报修单通过工单详情里的「报修单号」列溯源即可，**不再单设「报修单历史」页**（已删除）。

**落地结果**：后台「工单业务管理」组两项菜单——待受理报修单（派单入口，角标=待受理数）、工单管理（角标=进行中数）。工单管理下拉默认「全部状态」，无「进行中」多状态区间项（多状态过滤容易让用户困惑默认看到什么）。

**教训**：用户提的“删掉 X”诉求要顺着数据流回溯——删除入口前先确认被删的东西是不是别处唯一的依赖（这里派单是闭环唯一入口）。这条比“用户说删就删”更重要。
## D-025 | 2026-10-16：同浏览器多账号登录互相覆盖——身份看守 + 登录确认

**现象**：管理员点「生成报销单」后"跳到财务界面"。排查前端全部跳转点（仅 5 处，全部去 login.html）与后端接口，均无跳财务的代码。

**根因**：登录态存 localStorage（key=itsdUser），**同浏览器所有标签页/窗口共享**。用户先用 admin 登录、后用 finance 登录（另一窗口），finance 凭证静默覆盖 admin。原窗口随后任何页面重载（refresh/导航/bfcache 恢复）时 admin.html 重新读 localStorage，按财务角色渲染菜单与页面——看起来像"跳转"，实际是"重新初始化时身份已被换"。

**决策**：两端防护——
1. **login.html 登录前确认**：提交登录时若本地已有其他账号（username 不同）在线，confirm 明确告知"将以 X 身份替换 Y（其它窗口随之切换）"，用户可取消。把静默覆盖变成显式选择。
2. **admin.html 跨窗口身份看守**：监听 storage 事件，itsdUser 被改成其他 userId（或被清空）时弹提示并 replace 回登录页。已打开的窗口不再静默换角色，而是明确失效。

**不做**：按标签页隔离登录态（sessionStorage）—— 后台/维修端/科室端同域互跳依赖共享登录态，隔离会破坏"登录一次三端可用"的体验；且维修工作机/科室工作机都是单账号设备，多账号并存是低频场景，确认+看守足够。
**修订（同日）**：登录前 confirm 方案被用户否定——财务登录被弹窗拦截，正常单账号流程反而被打断。已改为：**登录页静默覆盖旧登录态（撤掉 confirm），保留 admin.html 的 storage 身份看守**（旧窗口提示失效并回登录页）。教训：跨窗口身份冲突是低频场景，不应让高频的普通登录为它付出弹窗代价；防护放在"受影响的旧窗口"一侧，而不是"发起登录的新窗口"一侧。
## D-026 | 2026-10-16：报销单按科室分池 + 勾选生成（取代按月一键生成）

**背景**：用户给出《报销单设计》文档，核心诉求是「按科室分池管理待报销工单，管理员手动勾选工单生成报销单」。此前实现是「按月一键全量生成」，已暴露问题：同科室第二张工单无法报销（被"同科室同月已生成"规则跳过）、金额变动要作废旧单重来。

**决策（第 1 步落地，不过度设计）**：
- **后端**：ReimbursementService 加 poolSummary()（按科室聚合，只返回有数据的科室）+ generateFromPool(deptId, workOrderIds)（校验归属/状态/未占用后生成单张报销单，同科室最多一张待签字单）。
- **前端**：reimbursements.js 重写为科室池卡片网格 → 进入科室抽屉 → 勾选工单生成 + 已有报销单列表。旧的按月一键生成按钮删除。
- **不做**：撤回/退回（D-025 已加身份看守，退回流程后置）、快照隔离表（用报销单明细 + 工单状态机已够追溯）、拼音/编码搜索（68 科室下拉够用）、草稿合并、导出打印。

**与文档差异**：文档要求"生成瞬间快照隔离，新工单进入下一轮"——本实现用"工单被报销单明细占用后不可再被勾选"达到等价效果，不额外建快照表。文档要求"工单从待报销池移出"——前端按"是否被报销单明细占用"过滤展示，后端不删数据。

**遗留**：撤回/退回流程（文档第五节状态流转里的"已退回"）未实现，待第 2 步推进；报修端同步细化（第八节）未做，当前报修端已同步工单/报销单状态，够用。
## D-027 | 2026-10-16：报销单撤回/退回 + 报修端同步（D-026 第 2/3 步）

**第 2 步：撤回/退回状态机**
- ReimbursementStatus 加 RETURNED(已退回)。完整流转：
  - 生成 → PENDING_SIGN →（签字）→ PENDING_REIMBURSE →（财务回填）→ REIMBURSED
  - 未签字管理员撤回 → VOIDED（工单回退入池）
  - 护士长退回 → RETURNED（工单回退入池，PENDING_SIGN 状态退回）
  - 财务退回 → RETURNED（工单回退入池，PENDING_REIMBURSE 状态退回，已签字无需重签）
- generateFromPool 占用校验调整：只算被"活跃报销单"（待签字/待报销/已报销）占用的；RETURNED/VOIDED 的旧占用放行，工单可重新勾选。
- 三个新接口：POST /revoke（管理员撤回）、/return-finance（财务退回）、/return-nurse（护士长退回）。
- 前端：reimbActions 按 status+role 渲染撤回/财务回填/财务退回按钮；科室端 report.html 签字弹窗加「退回管理员」按钮（仅报销单签字时显示，工单验收签字隐藏）。

**第 3 步：报修端同步细化**
- report.html loadHistory 并发拉本科室报销单列表 + 逐张拉详情建 workOrderId→reimbursement 反查索引（小规模 < 20 张可接受，不做后端聚合接口避免过度设计）。
- 历史列表里已完成/已报销工单行追加「报销单 #N · 状态 · 月份」行（文档第八节：报修端同步报销状态与关联单号）。
- 同工单关联多张报销单时按优先级取最高展示（已报销 > 待报销 > 待签字 > 已退回 > 已作废）。
- REIMB_STATUS 字典加 VOIDED/RETURNED。

**遗留**：财务退回后工单回退到 PENDING_REIMBURSE（已签字），管理员重新生成新单时不需要护士长重签——这条已由占用校验调整覆盖。但若财务退回后想"修改金额再生成"，需要管理员先撤回旧单（已 RETURNED 不可撤回，只能重新勾选生成新单，旧单保持 RETURNED 留痕）。
## D-028 | 2026-10-17：部署形态定为「院内内网单机部署」，不做公网云

**来源：用户明确要求 + 用户选择**

> 原话："部署上云必须最先做，否则手机会访问不到，企业微信通知里的链接也打不开。"
> 原话（选择资源时）："什么都没有（无服务器、无域名、无公网 IP）"
> 原话（手机网络）："基本都在医院内网 WiFi 下（推荐按此判断）"
> 原话（数据口径）："保留结构 + 基础数据（科室/账号），业务数据清空（推荐）"
> 原话（期限）："今天内（2-3 小时，先能打开再优化）"

**决策**：用户无云服务器/域名/公网 IP，且手机都在院内 WiFi → 「上云」在成本与合规上都不成立，改为**本机常驻 + 内网直连**。公网暴露含科室/维修/报销数据的系统本身也不合适。技术上不做任何妥协：配置全部外置为环境变量，将来换云只需改 `DB_URL`/`FILE_UPLOAD_DIR`/`NOTIFY_PUBLIC_HOST`。

**关键事实：`notify.publicHost` 为空 = 企微通知链接打不开的根因**
用户 8080 上的 IDEA 实例 `/api/system/info` 实测 `notify.publicHost: ""`，`wecomEnabled: false`。通知消息里拼不出可点地址。启动脚本已改为自动探测内网 IP 注入该值。

**本轮踩到的 5 个坑（都不是业务 bug，是部署环境坑）**
1. **PowerShell 脚本必须存成 UTF-8 with BOM**：Windows PowerShell 5.1 按 GBK 读 .ps1，中文注释会把字符串提前截断，报 "The string is missing the terminator"。用 `pwsh` 7 跑没事，但用户双击/`powershell -File` 会炸。
2. **`JAVA_HOME` 默认是 jdk8** → `UnsupportedClassVersionError: class file version 61.0`。脚本改为遍历候选 JDK 并**实测 `java -version` 主版本 ≥17** 才用；不能用"JAVA_HOME 存在即通过"。
3. **`java -version` 走 stderr**，而脚本顶部 `$ErrorActionPreference='Stop'` 会把它当异常，导致探测函数静默失败 → 全部候选被跳过。需在函数内临时改回 `Continue`。
4. **DSH 沙箱下 CIM 受限**：`Get-NetTCPConnection` / `Get-NetFirewallProfile` / `Get-CimInstance Win32_Process` / `tasklist` 全部静默失败或 Access denied（只看得到 12 个进程）。→ **端口占用检查不能只信 `Get-NetTCPConnection`，要用 `netstat -ano`**；防火墙状态在沙箱内根本查不了。
5. **内网 IP 探测必须排除虚拟网卡**：`ipconfig` 首个 IPv4 是 WSL/Hyper-V 的 `172.23.240.1`，真实 WLAN 是 `10.31.67.107`。改用 .NET `NetworkInterface` 过滤 `Wireless80211/Ethernet` 且 Description 不含 Virtual/VMware/Hyper-V/WAN Miniport。

**其它实测结论**
- 手机走 `http://10.31.67.107:8080` 时 **PWA 不可用**（service worker 需安全上下文；页面已有 `'serviceWorker' in navigator` 守卫，不会报错，只是不能"添加到主屏"）。要 PWA 必须上 HTTPS。
- 本机 `Invoke-WebRequest http://10.31.67.107:8080` 返回 200 **只能证明监听在 0.0.0.0、没有绑 localhost**，**不能证明手机可访问**（同机访问不经入站防火墙规则）。手机实测是唯一验收。
- QR 码由 `QrController` 按请求 `Host` 头动态拼 URL（`/api/qr?path=...&token=token-<userId>`），换端口二维码自动跟着变，无硬编码。
- 源码无任何硬编码 `10.31.67.107` / `:8080`（grep 已确认）。
- 源码 `sys_user` 实测 6 行、`department` 68 行；`information_schema.table_rows` 是估算值（曾显示 5 行）。**取真实行数必须 `COUNT(*)`**。
- 8080 上跑的是用户 14:06 在 IDEA 启动的开发实例，沙箱内不可见/不可管理 → 正式部署先用 8081 并存验证，避免误杀用户进程。