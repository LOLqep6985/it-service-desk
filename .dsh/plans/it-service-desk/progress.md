# 进度日志：it-service-desk

## 会话记录

### 2026-10-13：项目定义与决策

- 完成：解析两份 ChatGPT 对话 PDF，确定简历项目方向与技术栈；检查开发环境；确认项目位置/技术栈/Redis 策略
- 结果/输出：项目定位「企业 IT 服务台与设备维修管理系统」；路线图 M1-M5 / Step 1-11
- 错误：无

### 2026-10-13：纳入业务约束与一周开发规划

- 完成：解析追加对话 PDF（追加1.pdf）；提取多维修人员/移动优先/三院区/网络异常/一周开发等约束
- 结果/输出：项目定位升级为「多院区现场 IT 运维工单与调度系统」；开发策略改为「M0 最小验证 → 7 天完整开发」

### 2026-10-13：M0 最小验证开发与测试通过

- 完成：
  - 排查 Maven 离线构建问题（本地仓库 D:\Maven\repository，JAVA_HOME 需 JDK17）
  - 创建 Spring Boot 2.7.18 工程：16 个 Java 源文件 + 4 个静态 H5 页面 + pom.xml + application.yml
  - 处理两个环境问题：删除 1653 个 _remote.repositories 元数据文件；pom 覆盖 lombok 版本 1.18.46；JAVA_HOME 指向 jdk17
  - 离线构建成功：mvn -o clean package -DskipTests → desk-0.1.0.jar
  - 启动应用（端口 8080），curl 闭环测试全部通过
- 结果/输出：M0 垂直切片跑通「登录→创建工单→今日任务→详情→提交维修→关闭」；静态页面 200
- 错误：无（构建期错误已解决，见 findings.md）
- 里程碑进展：M0 complete 达成
- 下一步：Day 1 完整数据库设计（需先解决 MySQL 驱动依赖）

## Git 提交记录

| 日期 | 提交 | 内容 |
|---|---|---|
| 2026-10-13 | （待提交） | feat: M0 最小验证 - 工单闭环垂直切片 |
### 2026-10-13：IDEA 工程就绪

- 完成：用户在 IDEA 中加载 Maven（阿里云镜像 + 系统仓库 D:\Maven\repository）、选择 JDK17 作为 SDK；确认项目名/包名保留
- 结果/输出：项目可在 IDEA 中导入、编译；等待 Day 1 MyBatis-Plus 持久层开发
- 下一步：Day 1 实体/Mapper/Service + MySQL 就绪验证
### 2026-10-13：Day 1 完成（MyBatis-Plus + MySQL 持久层）

- 完成：
  - 编写 schema.sql（建表：sys_user / department / work_order）与 data.sql（种子数据 5 用户 + 5 科室）
  - 实体加 MyBatis-Plus 注解（@TableName/@TableId）
  - 新增 mapper 包（UserMapper / DepartmentMapper / WorkOrderMapper 继承 BaseMapper）
  - 主类加 @MapperScan；WorkOrderService / AuthController 改用 Mapper 访问真实数据库
  - 删除 InMemoryStore（内存存储退役）
  - application.yml 增加 spring.sql.init（每次启动自动建表+种子数据）
- 结果/输出：闭环测试通过；MySQL 落库验证（work_order 有记录，sys_user=5，department=5）
- 错误：无（mysql 命令行中文乱码仅为终端编码问题，数据 utf8mb4 正常）
- 里程碑进展：M1 complete；Day 1 complete
- 下一步：Day 2 JWT 登录 + RBAC + 全局异常处理

## Git 提交记录

| 日期 | 提交 | 内容 |
|---|---|---|
| 2026-10-13 | （待提交） | feat: Day1 MyBatis-Plus + MySQL 真实持久层 |
### 补充：报修按钮权限控制（后端强制）

- 完成：WorkOrderController 增加 X-Token 鉴权；创建报修仅 REPORTER、提交维修仅 REPAIRER；前端三页携带 token
- 验证：报修人创建成功 / 维修工创建 403 / 匿名 401 / 管理员创建 403 / 维修工今日任务成功 / 报修人维修 403 / 维修工维修成功
- 下一步：Day 2 数据模型重构（三端流程）
### 2026-10-13：Day 2 完成（报修单/工单分离数据模型）

- 完成：
  - schema.sql 新增 department_binding / report / consumable / reimbursement 四表；work_order 增加 report_id/sign_status/signed_by/signed_at
  - 新增实体 Report/DepartmentBinding/Consumable/Reimbursement + 对应 Mapper + 枚举（ReportStatus/SignStatus/ReimbursementStatus）
  - ReportService（创建报修单/列表/派单生成工单）+ ReportController（仅报修人提交、仅管理员/调度查看与派单）
  - create.html 重写为极简报修单表单（报修人姓名/设备名称/照片/描述/科室负责人）
- 结果/输出：验证通过——报修人提交 R000001(PENDING) → 维修工提交 403 → 管理员查看 → 派单生成工单(report_id=1, UNSIGNED) → 维修工今日任务 → 提交维修完成；MySQL 落库正确
- 里程碑进展：M1 complete；Day 2 complete
- 下一步：Day 3 科室报修端免登录

## Git 提交记录

| 日期 | 提交 | 内容 |
|---|---|---|
| 2026-10-13 | （待提交） | feat: Day2 报修单/工单分离数据模型 |
### 2026-10-13：Day 3 完成（科室免登录报修端）

- 完成：
  - DepartmentBinding API：管理员/调度绑定设备码↔科室；/api/bindings/info 设备码识别科室
  - 免登录报修：POST /api/reports/anonymous（设备码验证→科室自动带出）+ GET /api/reports/by-device（本科室历史）
  - 文件上传：POST /api/files/upload（保存到 uploads/）+ GET /uploads/{filename}（控制器方式提供，修复静态映射404）
  - report.html 科室免登录报修页（设备码进入/极简表单/照片上传/快捷标签/报修历史）
  - login.html 增加科室免登录入口
- 结果/输出：绑定 DEV001→科室1；免登录提交 R000002/R000003 自动带出科室；照片上传后 HTTP 200；维修工绑定设备 403
- 里程碑进展：M2 complete；Day 3 complete
- 下一步：Day 4 管理员后台

## Git 提交记录

| 日期 | 提交 | 内容 |
|---|---|---|
| 2026-10-13 | （待提交） | feat: Day3 科室免登录报修端 |
### 2026-10-13：修复科室报修页反馈问题（4项）

- 1. 历史记录 undefined：status 字段后端返回枚举名（如 PENDING），前端误用 .status.label → 已加状态/院区映射（REPORT_STATUS/CAMPUS_LABEL/WORK_ORDER_STATUS），today/detail/create 同步修复
- 2. 提交后表单未重置：新增 resetForm()（清空姓名/设备/描述/照片/标签）
- 3. 重复提交：后端 10 分钟防重（同科室+同设备名 10 分钟内禁止重复报修，返回「该设备报修已提交，请勿重复提交」）；前端提交后按钮 10 分钟倒计时禁用
- 4. UI：成功提示改为顶部 Toast；科室负责人改为只读自动带出（department 表新增 department_head 字段，绑定接口返回）
- 验证：DEV001 查科室返回负责人；打印机首次提交成功、立即重提被拒、换电脑可提交；登录式报修同样防重
### 2026-10-13：Day 4 完成（管理员后台）

- 完成：
  - UserController：GET /api/users（仅 ADMIN/DISPATCHER，可 ?role=REPAIRER 过滤，密码脱敏）
  - admin.html：PC 管理后台（待受理报修单表格+派单弹窗、报修历史、基础数据：科室+用户）
  - login.html：ADMIN/DISPATCHER 登录后跳转 admin.html
- 结果/输出：admin.html 200；管理员可查看维修工列表并派单（R000008→工单 id=5）；维修工访问用户列表 403
- 里程碑进展：M3 complete；Day 4 complete
- 下一步：Day 5 维修人员端

## Git 提交记录

| 日期 | 提交 | 内容 |
|---|---|---|
| 2026-10-13 | （待提交） | feat: Day4 管理员后台 |
### 2026-10-13：Day 5 完成（维修人员端）

- 完成：
  - 后端：POST /api/work-orders/{id}/accept（接单：ASSIGNED→IN_PROGRESS）；POST /repair 扩展支持 consumables 列表（耗材/欠条写入 consumable 表）
  - 权限：接单/维修仅 REPAIRER，且必须工单派给本人（否则 403）
  - 前端：today.html 卡片加「接单」按钮；detail.html 重写（接单按钮/维修记录+耗材欠条动态行/签字状态展示）
- 结果/输出：派单→李工尝试接张工单 403→张工接单 IN_PROGRESS→提交维修（硒鼓120元欠条+搓纸轮45元）→COMPLETED/UNSIGNED→consumable 表落库
- 里程碑进展：M4 complete；Day 5 complete
- 下一步：Day 6 签字确认 + 月度报销

## Git 提交记录

| 日期 | 提交 | 内容 |
|---|---|---|
| 2026-10-13 | （待提交） | feat: Day5 维修人员端 |
### 2026-10-13：修复管理员后台不显示工单（补充功能）

- 问题：admin.html 之前只显示报修单，没有显示工单（work_order）
- 修复：新增 GET /api/work-orders（仅 ADMIN/DISPATCHER，可 ?status 过滤）+ admin.html 增加「工单管理」区块（工单ID/报修单号/设备/科室/维修工/状态/签字/时间）
- 验证：管理员看到 6 条工单（含报修单号、状态、签字）；维修工访问 403；admin.html 200
- 说明：维修工姓名用 /api/users?role=REPAIRER 映射显示
### 2026-10-13：Day 6-1 完成（工单详情抽屉）

- 后端：GET /api/reports/{id}（管理员/调度员查看报修单详情）；GET /api/work-orders/{id}/consumables（管理员/调度员/该单维修工查看耗材）
- 前端：admin.html 工单列表加「详情」按钮；右侧抽屉展示：基本信息/报修信息（照片可点击放大）/流转记录/维修记录/耗材更换记录/签字状态；空值显示「未填写/暂无」
- 验证：报修单 R000015 详情正常；工单#8 耗材（碳粉40元+搓纸轮25元）正常；admin.html 200
### 2026-10-13：Day 6-2 完成（签字闭环 + 核价）

- 后端：
  - WorkOrderStatus 增加 PENDING_ACCEPT（待科室验收）；维修工提交后工单进入待验收
  - work_order 增加 signature_url 字段
  - 科室端免登录接口：GET /api/work-orders/pending-accept?deviceCode=；GET /api/work-orders/{id}?deviceCode=；POST /{id}/sign
  - 管理员核价：POST /api/work-orders/{id}/consumables/pricing
- 前端：
  - detail.html：耗材只填名称+数量（去掉单价/欠条）；提交后提示「已提交科室验收」
  - report.html：新增「待验收工单」列表 + 手写签名弹窗（Canvas/代签/清除/确认）
  - admin.html：工单详情抽屉耗材可编辑实际单价 + 保存核价
- 验证：报修单 R000017(科室1) → 派单#10 → 接单维修 → PENDING_ACCEPT → DEV001 看到待验收 → 王护士长签字 → COMPLETED/SIGNED → 代签「王护士长(由张护士代签)」 → 核价电源线50元 全部通过
### 2026-10-13：Day 6-3 完成（工作留痕：手写签名展示 + uploads 路径修复）

- 科室端：新增「已完成工单」卡片 + 详情弹窗（维修记录/耗材/签字人/时间/手写签名图片）
- 管理员后台：工单详情抽屉「签字状态」增加手写签名图片展示（点击放大）
- 后端：GET /api/work-orders/by-department?deviceCode=&status=（科室端查本科室工单）
- 修复：uploads 目录改为绝对路径（），解决 IDEA 与沙箱工作目录不同导致签名图 404
- 验证：签名图片 HTTP 200；科室端详情返回 signatureUrl

### 2026-10-13：Day 6-4 完成（月度报销单闭环 + 全流程联调）

- 后端：
  - schema：reimbursement 增加 reimbursed_at；新增 reimbursement_item 关联表（报销单↔工单）
  - ReimbursementService：按月汇总 COMPLETED+SIGNED 工单 → 按科室分组 → 合计耗材金额 → 生成报销单（同科室同月幂等）
  - ReimbursementController：生成/列表/详情/科室签字/财务回填；仅 ADMIN/DISPATCHER，科室端凭 deviceCode
- 前端：
  - admin.html：新增「月度报销单」区块（月份选择 + 生成按钮 + 列表 + 详情抽屉 + 财务回填按钮）
  - report.html：科室端新增「月度报销单」卡片（待签字→签字确认复用 Canvas 签名；查看明细含关联工单）
- 验证（全流程联调通过）：
  - 科室1：已有 4 张签字工单 → 生成报销单 ¥70（电源线50 + 水龙头2×10）→ DEV001 科室端可见 → 王护士长签字 → 财务回填 REIMBURSED
  - 科室2 全新流程：绑定 DEV002 → 报修 R000021 → 派单 #14 → 接单 → 维修+耗材(主板电池) → 核价 ¥120 → DEV002 签字 → 生成报销单 ¥120（科室1跳过）
  - 权限：维修工生成/查看报销单 403；重复生成同月返回空；无签字月份返回空
  - 页面：admin.html / report.html 等 6 个页面均 HTTP 200
- 里程碑进展：M5 签字+报销闭环完成（待 Day7 部署/简历）
- 下一步：Day 7 部署配置 + README + 简历项目描述