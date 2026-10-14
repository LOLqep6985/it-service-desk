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