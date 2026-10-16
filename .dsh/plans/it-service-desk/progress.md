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

### 2026-10-13：推送云端前准备（Day 6-5）

- application.yml 数据库连接改为环境变量占位符：DB_URL / DB_USERNAME / DB_PASSWORD（保留本地默认值，行为不变）
- 新增 README.md：项目简介、技术栈、流程状态机、快速启动、演示账号、目录结构
- 检查确认：无远程仓库、分支 master、.gitignore 已排除 target/.m2repo/uploads/.idea/.rdoc_inspect，跟踪文件仅源码+文档
- 下一步：用户在 Gitee 创建空仓库 → git remote add origin + git push -u origin master

### 2026-10-13：Git 工作流切换为「用户手动 commit/push」（VSCode Git Graph）

- 用户要求：以后每次变更代码并保存进度后，AI 只告知修改内容与建议提交信息，由用户在 VSCode 中手动 commit + push
- 已更新：AGENTS.md 强制规则、findings.md 决策记录
- 当前未提交变更：AGENTS.md、findings.md、progress.md（等待用户手动 commit）

### 2026-10-13：体验优化——PWA 化 + 科室端首次绑定（待用户 commit）

- 完成：
  - PWA 资源：manifest-report.json（科室报修/绿）+ manifest-repairer.json（维修工作台/蓝）+ service-worker.js（导航网络优先/静态缓存优先/API 不缓存）
  - 图标：icons/report-192/512.png（绿底「报」）、icons/repair-192/512.png（蓝底「修」）
  - 4 页接入：report/today/detail/login 增加 manifest/theme-color/apple-touch-icon + SW 注册
  - 科室端绑定优化：首次使用引导页（设备码输入 + 说明）、?deviceCode= 自动绑定（扫码预留）、「重新绑定」按钮、绑定成功展示全部卡片
- 验证：
  - 构建成功；manifest 以 application/json、SW 以 application/javascript、图标以 image/png 返回
  - report/today/detail/login 均 HTTP 200；登录接口正常；bindings/info?deviceCode=DEV001 返回科室1
- 踩坑：`.webmanifest` 被 Spring 以 application/octet-stream 返回 → 改名 `.json` 解决
- 说明：SW 仅在 HTTPS/localhost 生效；局域网 http 不影响正常使用
- 待办：用户在 VSCode 手动 commit + push（本次所有改动尚未提交）

### 2026-10-13：需求变更——去掉设备码，科室端两级下拉选科室（待用户 commit）

- 完成：
  - 科室表导入《医院科室楼层索引.md》68 个真实科室（东区39 / 老院23 / 西院6），命名「建筑-楼层-科室」，name 唯一键幂等
  - 后端所有科室端接口支持 departmentId（匿名报修/报修历史/待验收/已完成/详情/耗材/签字/报销单），保留 deviceCode 兼容
  - report.html 绑定页改为「院区→科室」两级下拉；绑定后存 localStorage（itsdDepartmentId/itsdDepartmentName）并刷新，显示「当前科室：XXX」
  - 提示文案「这是本科室专用工作机。请下拉选择您所在的科室…」；错误提示「请选择科室」
  - 科室负责人：有默认值只读，无默认值可手动填写
- 验证（全链路通过）：
  - /api/departments 返回 68 个科室；页面/manifest/SW 均 200
  - 科室「门诊楼-三层-五官科诊室」(id=13)：匿名报修 R000023 → 历史查询 → 派单 #16 → 接单 → 维修+耗材 → 待验收 → departmentId 签字 → 已完成，全部通过
  - 测试数据已清理（保留历史演示数据）
- 说明：旧演示数据引用已删除的旧科室 id，但表内有科室名快照，展示不受影响
- 待办：用户在 VSCode 手动 commit + push（本次所有改动尚未提交）

### 2026-10-13：今日收尾（已提交 + 保存进度）

- 用户已在 VSCode 提交并推送：
  - `9b09824 保存项目进度`（PWA 化 + 绑定引导 + 项目记忆文档）
  - `1b9cafa feat: 科室端去掉设备码，改为「院区→科室」两级下拉绑定 + 导入真实楼层索引68科室`
- 工作区干净；MySQL 中 department=68（真实楼层索引），旧演示科室已清理
- 明日待办：Day 7 部署配置（Docker/Nginx）+ README 完善 + 简历项目描述 + 最终演示（详见 task_plan.md Next Step）

---

> 以下条目按 project-memory **v3.0.0** 规范记录（事实层只追加）。

## 会话记录（v3 编号）

### S-001 | 2026-10-14：采用 project-memory v3 规范 + 复制新版 skill 到工作区

- 完成：
  - 将新版 project-memory skill（v3.0.0，位于 `.dsh/skills/project-memory`）复制到 `D:\DeepSeek_data\gradaute_space\skills\project-memory`
  - 确认旧版 v2.0.0 已备份在 `skills/_backup_project-memory_v2`（含 `.zip` 存档）
  - 按 v3 规范追加记忆：findings.md 新增 D-001/D-002；progress.md 新增本会话 S-001；handoff.md 重写为指针式
- 结果/输出：项目记忆规范升级为 v3（事实层只追加 + 指针式交接）
- 错误：无
- 里程碑进展：无新功能开发；M5 仍待 Day 7 收尾
- 下一步：Day 7 部署配置（Docker/Nginx）+ README 完善 + 简历项目描述（见 task_plan.md Next Step）

## Git 提交记录（v3 追加）

| 日期 | 提交 | 内容 |
|---|---|---|
| 2026-10-13 | 9b09824 | 保存项目进度（PWA 化 + 绑定引导 + 项目记忆） |
| 2026-10-13 | 1b9cafa | feat: 科室端去掉设备码，改为「院区→科室」两级下拉绑定 + 导入真实楼层索引68科室 |
| 2026-10-14 | （待用户提交） | docs: 采用 project-memory v3 规范 + 复制新版 skill 到工作区 |

### S-002 | 2026-10-14：新会话恢复状态（Day 7 启动前全量通读）

- 完成：
  - 按 project-memory v3 第一步恢复：读 `AGENTS.md` → `.active_plan`(=it-service-desk) → `handoff.md` → `task_plan.md` → `findings.md` → `progress.md`
  - 全量通读代码：pom/schema.sql/data.sql/application.yml；6 个 controller + 3 个 service + 8 个 mapper + 13 个 model + 12 个 DTO；7 个 H5 页面（admin/report/today/detail/create/login/index）+ service-worker + 2 个 manifest
  - 核查环境与版本库：`git log/status/diff`、远程与本地分支、MySQL80 服务、8080 端口、JDK 与 jar 产物
- 结果/输出：
  - 确认当前处于 **Day 1-6 全部完成、Day 7 未启动**；M0-M4 完成、M5 待「部署+简历」收尾
  - 修正 `handoff.md` 的陈旧条目（原文写「项目记忆 4 文档待提交」，实际已在 a74be2b 提交）→ 改为指向 `findings.md#D-003`
  - 新增 `findings.md#D-003`：记录远程仓库已改为 GitHub、环境可用性、无测试/无 Docker 现状，以及 5 项已知代码缺口
- 错误：
  - 追加 D-003 时误用 `edit` 把 `### D-002` 标题替换掉，导致 D-002 正文短暂失去标题 | 尝试：定位并核对被改区域 | 解决：先原样恢复 D-002 标题与正文，再把 D-003 追加到文件末尾；已复核 D-001/D-002/D-003 顺序与内容完整
- 里程碑进展：无新功能开发；M5 仍待 Day 7 收尾
- 下一步：Day 7 部署配置（Docker/Nginx）+ README 完善 + 简历项目描述（见 `task_plan.md` 的 `## Next Step`）

## Git 提交记录（S-002 追加）

| 日期 | 提交 | 内容 |
|---|---|---|
| 2026-10-14 | （待用户提交） | docs: 恢复 Day 7 前状态 + 现状核查（findings D-003 / handoff 修正 / progress S-002） |

### S-003 | 2026-10-14：科室端绑定改三级联动（院区→楼栋→科室）+ 楼层标签

- 需求来源：用户在启动验证前提出（详见 `findings.md#D-004` 的用户原话逐字）
- 完成：
  - 数据层：`department` 新增 `floor`；新增幂等迁移脚本 `sql/2026-10-14-department-floor.sql`；`schema.sql` 加列；`data.sql` 追加幂等回填（中文楼层→1F..12F）
  - 后端：`Department` 加 `floor`；`DepartmentController.list()` 固定按 campus→building→id 排序
  - 前端 `report.html`：绑定卡改为「院区(select) → 楼栋(select) → 科室(自定义下拉面板)」；第一级变化清空二三级并加载楼栋；第二级变化清空三级并加载科室；第三级每行=蓝色 `.floor-chip` 楼层标签 + 深色科室名；已绑定后「当前科室」也带楼层标签 + 院区·楼栋
- 验证（证据）：
  - 迁移执行成功：`total=68, with_floor=68`；接口 `/api/departments` 返回 floor、无缺失、排序稳定
  - `node .rdoc_inspect/test-3level.js`（Node + 最小 DOM 桩，跑真实接口数据）→ **ALL CHECKS PASSED**：楼栋 2 项、门诊楼 20 科室 20 标签、3F 标签存在、科室名已去前缀、改院区后二三级清空、6 楼栋合计 68 科室 0 缺标签
  - 幂等回填：`UPDATE department SET floor=NULL WHERE id=13` → 重启应用 → 接口返回 `floor=3F`（全新库同样自动回填）
  - `/report.html` HTTP 200，含 `buildingSelect` / `deptPickerPanel` / `.floor-chip`
  - 构建：`mvn -o '-Dmaven.repo.local=.m2repo' package '-DskipTests'` → BUILD SUCCESS
- 错误：
  - Maven 参数被 PowerShell 拆解（`-Dmaven.repo.local=.m2repo` 报 Unknown lifecycle phase）| 尝试：直接拼接 | 解决：给 `-D` 参数加单引号
  - 三级联动测试首轮 B/E 段失败 | 尝试：怀疑页面逻辑 | 解决：定位为测试桩同步执行、`change` 监听注册在 `init()` 的 `await` 之后；桩内加 `await setTimeout 50ms` 后全通过（非产品缺陷）
  - 测试桩缺 `canvas.getContext` 导致脚本顶层报错 | 解决：桩内补 `getContext()` 空实现
- 备注：临时验证实例跑在 **8090**（避开用户 8080），验证后已 `Stop-Process` 清理 java 进程；`.rdoc_inspect/test-3level.js` 保留作回归脚本（该目录已 gitignore）
- 里程碑进展：属于 M5 前的体验优化；M5 仍待 Day 7 部署/简历收尾
- 下一步：用户启动验证三级联动效果并反馈；随后进入 Day 7（部署 + README + 简历）

## Git 提交记录（S-003 追加）

| 日期 | 提交 | 内容 |
|---|---|---|
| 2026-10-14 | （待用户提交） | feat(dept): 科室端绑定改三级联动（院区→楼栋→科室）+ department.floor 楼层标签 |

### S-004 | 2026-10-14：三级联动抽为共用组件 + create.html 接入 + 管理后台基础数据分类

- 需求来源：用户本轮两条指令（详见 `findings.md#D-005` 用户原话逐字）
- 完成：
  - 新增 `static/js/department-cascade.js`（`window.DeptCascade`：`create()` 三级联动、`buildingOf/floorOf/shortNameOf/floorChip/campusLabel`、`groupByLocation()` 院区→楼栋→楼层分组）与 `static/css/dept-cascade.css`（`.picker*`/`.floor-chip`）
  - `report.html`：删除内联联动逻辑与样式，改为调用组件（三页共用一份实现）
  - `create.html`：接入三级联动；选科室自动带出科室负责人（有默认值只读/无则手填）；提交改用所选科室 `departmentId`
  - `admin.html`：「基础数据」拆为「科室索引」+「用户」两块；科室按 院区→楼栋→楼层 分类（楼层蓝色标签、科室名 chip、每级数量、楼层数字升序），用户显示角色彩色标签
  - `service-worker.js`：`CACHE` 升级 `itsd-v1` → `itsd-v2`，新资源加入 `APP_SHELL`
- 验证（证据）：
  - `node .rdoc_inspect/test-frontend.js` → **ALL CHECKS PASSED（34 项）**：语法 11 项；组件 13 项；report.html 5 项；create.html 8 项（含"未选科室不发请求"）；admin.html 7 项
  - HTTP：`/css/dept-cascade.css` 200 + text/css、`/js/department-cascade.js` 200 + application/javascript、report/create/admin 三页 200
  - 构建：`mvn -o '-Dmaven.repo.local=.m2repo' package '-DskipTests'` → BUILD SUCCESS，新资源已进 `target/classes/static`
- 错误：
  - 测试桩中 `els.reporterName` 为 undefined（页面只在提交时才 `getElementById`，桩元素懒创建）| 尝试：直接取属性 | 解决：测试改用 `document.getElementById(...)` 触发懒创建
  - CSS 注释里「原生/原生」写法怀疑有错别字 | 尝试：edit 替换 | 解决：工具提示两串相同 → 复核确认本就一致，无需改（未产生脏改动）
- 里程碑进展：M5 前的体验优化；M5 仍待 Day 7
- 下一步：用户启动验证（三级联动 + 管理后台基础数据）；README 更新按用户要求推迟到 Day 7

## Git 提交记录（S-004 追加）

| 日期 | 提交 | 内容 |
|---|---|---|
| 2026-10-14 | （待用户提交） | refactor(web): 三级联动抽为共用组件 + create.html 接入 + 管理后台基础数据分类 |

### S-005 | 2026-10-14：后台重构 批次 1 —— `/api/stats/dashboard` 聚合接口

- 需求来源：用户要求重构管理后台布局，方案见 `findings.md#D-006`；开工方式为「按批次 1→4，每批汇报等验证」
- 完成：
  - `dto/DashboardStats.java`：概览指标（今日/昨日新增、待受理、已派单、处理中、进行中合计、待验收、本月已完成、本月报销金额与三种状态笔数、近 7 日趋势 TrendPoint）
  - `service/StatsService.java`：SQL 侧聚合——`selectCount` 计数、`QueryWrapper.select("DATE(created_at) AS d","COUNT(*) AS c").groupBy(...)` 出趋势、本月报销按 `month` 取列表求和；含 `toDateKey()` 兼容不同驱动的日期返回类型
  - `controller/StatsController.java`：`GET /api/stats/dashboard`，仅 ADMIN/DISPATCHER
- 验证（真值对账，证据可复现）：
  - 应用跑在 8090，`GET /api/stats/dashboard` + `X-Token: token-1` → code=0
  - 指标 vs MySQL 真值**逐项一致**：今日 0/0、昨日 21/21、待受理 10/10、已派单 3/3、处理中 0/0、待验收 1/1、本月已完成 10/10、本月报销 1 笔 ¥70.00/1 笔 ¥70.00、已报销 1/1、待签字 0/0
  - 趋势：SQL `GROUP BY DATE(created_at)` 仅 2026-10-13=21，接口 trend 同为 10/13=21，其余 6 天 0（含今日 10/14=0）
  - 权限：维修工 token-3 → **403**「无权限：只有管理员或调度员可以查看概览数据」；无 token → **401**
  - 构建：`mvn -o '-Dmaven.repo.local=.m2repo' package '-DskipTests'` → BUILD SUCCESS
- 错误：无
- 里程碑进展：M5 前体验优化（后台重构批次 1/4）
- 下一步：等用户验证后进入批次 2（前端基座 + admin.html 薄壳 + 空视图占位）

## Git 提交记录（S-005 追加）

| 日期 | 提交 | 内容 |
|---|---|---|
| 2026-10-14 | （待用户提交） | feat(stats): 新增后台概览聚合接口 /api/stats/dashboard（指标+近7日趋势+本月报销） |

### S-006 | 2026-10-14：后台重构 批次 2 —— 前端基座 + admin.html 薄壳 + 工作台视图

- 需求来源：`findings.md#D-006`（方案）、`#D-007`（前端架构决策）
- 完成：
  - 新增 `css/admin-layout.css`：设计变量（深色侧栏/科技蓝/浅灰底）、布局（侧栏/顶栏/内容）、指标卡、表格、徽标/按钮/表单、抽屉、弹窗、Toast、空态、分页、骨架屏、纯 CSS 趋势柱状与占比条、四档响应式
  - 新增 `js/admin/`：`api.js`（fetch 封装/401 登出、403 只提示）、`store.js`（用户/角标/科室与用户缓存/订阅）、`ui.js`（`h()` DOM 构造、metricCard、dataTable、drawer、modal、confirm、toast、trendBars、distBars、pagination、searchBox、filterBy、paginate、STATUS 字典、placeholderRoute）、`router.js`（hash 路由 + onLeave/destroy 生命周期 + 未知路由回工作台）、`layout.js`（菜单树集中定义、内联 SVG 图标、角标、折叠记忆 localStorage、<768px 抽屉+遮罩）、`app.js`（引导 + `refreshBadges()`）
  - **工作台视图 `views/dashboard.js` 为真实实现**：快捷操作条 + 4 张指标卡（今日新增带环比/待受理/进行中/待验收）+ 待办聚焦（待受理 Top5、待验收 Top5，含维修工姓名映射）+ 本月报销概览（金额 + 三状态占比条）+ 近 7 日报修量柱状图
  - 其余 7 个菜单：`placeholderRoute` 占位（批次 3 替换）
  - `admin.html`：**479 行 → 45 行薄壳**（布局骨架 + 15 个模块引入）
  - `service-worker.js`：`CACHE itsd-v2 → itsd-v3`，`APP_SHELL` 纳入 admin 全部静态资源（32 条预缓存）
- 验证（证据）：
  - **新增 `jsdom` 集成测试**：`node .rdoc_inspect/test-admin-app.js`（真实 DOM + 真实后端接口）→ **ALL CHECKS PASSED（33 项）**：壳启动、一级菜单 4 组/二级 8 项及名称路由齐全、顶栏用户角色、角标（待受理 10 / 进行中+待验收 4 / 待签字 0 隐藏）、4 张指标卡数值与接口一致（0/10/3/1）、环比文案「↓21 较昨日」、7 根柱值与 trend 一致且今日柱标记、快捷操作 4 按钮、4 块概览卡、待办表 ≤5 行、桌面折叠与 localStorage 记忆、小屏抽屉开关、菜单点击改变 hash + 高亮 + 面包屑、**8 个路由全部渲染无错误态**、未知路由回退、无未捕获脚本错误
  - 回归：`test-frontend.js`（三级联动 + 报修端）仍 **ALL CHECKS PASSED**
  - HTTP：admin.html / admin-layout.css / js/admin/* 全部 200 且 MIME 正确；admin.html 已无内联业务脚本
  - 构建：BUILD SUCCESS
- 错误：
  - 编辑 `ui.js` 导出表时误删 `fmtTime/money/fmtDate` 一行 | 尝试：重新读取文件确认 | 解决：补齐该行并新增 `placeholderRoute`，复核导出表完整
  - Maven 打包失败：`repackage` 无法重命名 jar（应用正在运行占用文件）| 解决：先 `Stop-Process` 停应用再 `mvn package`；教训——**构建前先停应用**
  - jsdom 首次安装被拒（npm 缓存目录在沙箱外 `E:\CDriveGovernance\Caches\npm`）| 解决：`npm_config_cache` 指向工作区内 `.rdoc_inspect/.npm-cache`
  - 测试断言把两张待办表的行数合并统计（应分别校验）| 解决：改为按表分别断言（5 行 / 1 行）
- 里程碑进展：M5 前体验优化（后台重构批次 2/4）
- 下一步：等用户验证批次 2 的界面观感 → 批次 3 迁移 7 个视图（并再次升 SW 版本）

## Git 提交记录（S-006 追加）

| 日期 | 提交 | 内容 |
|---|---|---|
| 2026-10-14 | （待用户提交） | refactor(admin): 后台改「深色侧边栏+顶部导航+工作台仪表盘」布局（薄壳 + 基座模块 + 工作台视图） |

### S-007 | 2026-10-14：后台重构 批次 3 —— 7 个视图迁移 + 系统信息接口

- 需求来源：`findings.md#D-006`（批次计划）、`#D-008`（实现要点）
- 完成：
  - `ui.js` 新增可复用 `listSection()`（搜索/分页/计数一体化区块）
  - 迁移 7 个视图（原先 12 行占位 → 真实实现）：
    - `reports-pending.js` 140 行：待受理列表 + 2 张统计卡 + **派单弹窗**（维修工下拉，展示报修单摘要）
    - `work-orders.js` 205 行：状态过滤（5 项）+ 列表 + **详情抽屉 6 个分区**（含设备照片/签名图点击放大）+ **耗材行内核价保存**
    - `reports-history.js` 82 行：非待受理报修单 + 状态过滤 + 搜索/分页
    - `reimbursements.js` 197 行：月份选择 + 生成报销单 + 4 张状态统计卡 + 列表 + 详情抽屉（关联工单）+ **财务回填（二次确认）**
    - `departments.js` 108 行：4 张统计卡 + 院区→楼栋→楼层分组（6 栋 36 个楼层标签）+ 搜索（命中自动收起空分组）
    - `users.js` 87 行：角色过滤 + 4 张统计卡 + 列表（角色徽标，密码列显示「已脱敏」）
    - `system.js` 158 行：运行环境 / 数据规模 / 演示账号 / 业务状态机 / **已知限制（5 条，与 D-003 对齐）** / 前端模块
  - 新增后端 `GET /api/system/info`（`SystemInfoController`，仅 ADMIN/DISPATCHER，不返回密码）
  - `service-worker.js`：`CACHE itsd-v3 → itsd-v4`
- 验证（证据）：
  - `node .rdoc_inspect/test-admin-app.js` → **ALL CHECKS PASSED（86 项）**，新增覆盖：`/api/system/info` 401/403/200 权限三态；待受理页 10 行 + 每行派单按钮 + 弹窗维修工 2 个选项 + 取消后数据未改；工单页状态过滤 5 项 + 详情抽屉 6 分区齐全 + 耗材核价入口；历史页不含「待受理」；报销页月份选择/生成按钮/4 卡/表格行；科室页 4 卡 + 6 栋分组 + 36 楼层标签 + 短名；用户页角色徽标 + 密码脱敏；系统信息页 Java/端口/上传目录/演示账号/已知限制/状态机；**占位视图已全部替换**；未知路由回退；无未捕获脚本错误
  - 回归：`test-frontend.js`（三级联动 + 报修端）**ALL CHECKS PASSED**
  - HTTP：7 个视图 JS 与 admin.html 全部 200；SW 已确认为 `itsd-v4`
  - 构建：BUILD SUCCESS
- 错误：
  - `SystemInfoController` 初稿有两个笔误：`departmentBindings` 误用 `departmentMapper` 计数、枚举用全限定名且 `Department` 导入未使用 | 解决：改为导入枚举 + 删掉多余统计项与无用导入（构建前自查发现）
  - 批次 2 的 jar 占用教训起效：本批次改为**先停应用再 `mvn package`**，未再出现 repackage 失败
- 里程碑进展：M5 前体验优化（后台重构批次 3/4）
- 下一步：批次 4 全量回归 + 记忆落盘；随后 Day 7（部署 + README + 简历）

## Git 提交记录（S-007 追加）

| 日期 | 提交 | 内容 |
|---|---|---|
| 2026-10-14 | （待用户提交） | feat(admin): 迁移 7 个后台视图（派单/工单详情核价/报销/科室/用户/系统信息）+ 新增 /api/system/info |

### S-008 | 2026-10-14：后台重构 批次 4 —— 修复 5 条已知限制 + 全量回归收尾

- 需求来源：用户明确要求「已知限制：修」+ 批次 4 收尾（详见 `findings.md#D-009`）
- 完成（5 项修复）：
  1. 报销单签字留痕：`reimbursement.signature_url`（schema + 实体 + Service 落库）+ 管理员抽屉「签字留痕」分区 + 科室端报销明细展示签名图
  2. 维修端状态映射：`today.html` / `detail.html` 补 `PENDING_ACCEPT:'待验收'`
  3. 防重口径：改为「科室+设备+报修人+描述」3 分钟内一致才算重复；科室端倒计时 10 分钟 → 1 分钟并给出提示
  4. 工单详情按角色校验归属（维修工仅本人、报修人仅自己的单）
  5. 密码 BCrypt：引入 `spring-security-crypto`（BCryptPasswordEncoder）、`AuthController` 支持**旧明文登录后自动升级**、`data.sql` 种子改为哈希
  - 迁移脚本 `sql/2026-10-14-signature-and-bcrypt.sql`（幂等，已在当前库执行）
  - 系统信息页：原「已知限制」转为「近期修复」，并重新列出仍存在的 6 条限制
  - `service-worker.js`：`itsd-v4 → itsd-v5`
- 验证（证据）：
  - **新增 `test-batch4-api.js`（端到端）→ ALL CHECKS PASSED（24 项）**：5 个演示账号登录成功 + 错误密码被拒；全流程 R000024 → 工单#18（接单/维修/核价 ¥120/科室签字 COMPLETED）→ 生成报销单 → 科室签字 → **`signatureUrl` 落库并可从详情接口读回**；工单详情：管理员 200 / 本人 200 / 他人维修工 403 / 无关报修人 403；防重：**完全相同被拦、改描述可提交、换设备可提交**；科室端列表返回 signatureUrl
  - `test-admin-app.js`（jsdom）→ **ALL CHECKS PASSED（新增 5 项）**：报销单详情抽屉含「签字留痕」分区 + 签名图元素 `src=/uploads/acceptance-sign.png`
  - `test-frontend.js` → **ALL CHECKS PASSED（新增 4 项断言）**：today/detail 状态映射含 `PENDING_ACCEPT:'待验收'`、report.html 倒计时为 1 分钟、报销详情展示签名
  - 数据一致性：`API pending = 9 ; SQL pending = 9`（对账一致）
  - SW 已确认 `itsd-v5`；构建 BUILD SUCCESS
- 数据说明（重要）：
  - 端到端验收新增了一条**真实可演示数据**：报修单 R000024（李护士·打印机）→ 工单#18（已完成并签字）→ 报销单#3（五官科 ¥120，**待报销**）。它让工作台/报销页出现非零的「待报销」并可演示财务回填
  - 防重口径测试产生的 3 张报修单（R000025-27）**已清理**，避免污染演示数据
  - 顺带发现：用户在前两批验证期间点过「派单」，待受理数由 10 变为 9（属预期，非缺陷）
- 错误：无（迁移与验证一次通过）
- 里程碑进展：M5 前体验优化 + 已知缺陷清理（后台重构批次 4/4 完成）
- 下一步：Day 7 —— 部署配置（Dockerfile + docker-compose + Nginx）+ README 更新（含去掉陈旧的「设备码 DEV001」、补充 BCrypt 与迁移脚本说明）+ 简历项目描述

## Git 提交记录（S-008 追加）

| 日期 | 提交 | 内容 |
|---|---|---|
| 2026-10-14 | （待用户提交） | fix: 修复 5 条已知限制（报销签字留痕/维修端状态/防重口径/工单越权/密码BCrypt）+ 迁移脚本 |

### S-009 | 2026-10-15：清空业务数据以便用户跑完整闭环 + 空数据预检

- 需求来源：用户明确要求
- 用户原话（逐字）：> "我现在为了验证全流程，需要你将所有的工单，不管是已完成/未完成的订单数据，都全部清除，我要跑一遍完整的"报修-->分配工单-->完成工单-->科室签字-->后台显示工单完成(额外的-->总结每月报销单)"。"
- 执行（先备份后删除）：
  - **备份**：`mysqldump` 导出 5 张业务表 → `.rdoc_inspect/backup/demo-data-20261015-010733.sql`（15,215 字节，可回滚）
  - **清空**（按依赖顺序）：`reimbursement_item` → `reimbursement` → `consumable` → `work_order` → `report`，并 `ALTER TABLE ... AUTO_INCREMENT = 1` 重置编号（下次报修从 R000001 开始）
  - **保留**：`sys_user` 5 个演示账号、`department` 68 个科室、`department_binding`（0 行）
  - 清理前：report 22 / work_order 16 / consumable 10 / reimbursement 2 / reimbursement_item 5；清理后全部为 0
  - `uploads/` 保留 6 个历史文件（共 15.9 KB），已成孤儿但不会被界面引用；未删除，等用户决定
- 验证（新增空数据预检脚本 `test-empty-state.js`）→ **ALL CHECKS PASSED（21 项）**：
  - 接口：指标全 0、本月报销 ¥0.00、近 7 日趋势 7 点全 0、报修/工单/报销列表为空、科室仍 68、用户仍 5
  - 后台 8 页空态：工作台 4 卡显示「0单」无 undefined/NaN、趋势 7 根 0 值柱、待办区空态文案；待受理/工单/历史/报销页空态占位行正确；报销页 4 张统计卡仍渲染；科室页 6 栋分组不受影响；用户页 5 行；系统信息页正常
  - **全流程无未捕获脚本错误**
- 错误：
  - 空态断言误写（把 `.empty-row` 占位行也算作数据行，期望 0 行）| 解决：断言改为「占位行 1 + 数据行 0」
- 里程碑进展：为 M5 最终全流程演示准备干净数据基线
- 下一步：用户手动跑闭环；随后 Day 7 部署 + README + 简历

## Git 提交记录（S-009 追加）

| 日期 | 提交 | 内容 |
|---|---|---|
| 2026-10-15 | （无需提交） | 仅数据操作：清空业务表（代码与文档无变更） |

### S-010 | 2026-10-15：今日收尾（保存进度，明日继续）

- 完成：
  - 核对工作区：代码已全部提交至 `9e8f12f`（批次 1-4 依次为 `0f27158`、`4f39c5a`、`4caec14`、`9e8f12f`）；**工作区仅剩 2 个项目记忆文档待提交**
  - 更新状态层：`handoff.md`（当前状态 + 数据现状 + 已完成 S-009 + 下一步）、`task_plan.md`（当前阶段、重构批次表补提交号并标记 complete、`Next Step` 改为「明天从这里开始」、已完成与待办刷新）
- 更正（supersedes：`S-009` 的 Git 提交记录行）：
  - S-009 写「（无需提交）仅数据操作，代码与文档无变更」——**只对数据库操作本身成立**；该轮实际产出了 2 个记忆文档改动（`progress.md`、`handoff.md`），需要提交
- 明日开始的顺序（交给新会话）：
  1. 按 `handoff.md` 的「回读顺序」恢复：`AGENTS.md` → `.active_plan` → `handoff.md` → `task_plan.md`
  2. 优先等用户「完整闭环」的反馈；若要排查，先复现再定位（不猜、不盲改）
  3. 进入 **Day 7**：先向用户确认部署目标（本机 Docker 演示 or 内网 Linux + HTTPS），再做部署配置 + README 重写 + 简历项目描述
- 环境提醒（沿用既有教训）：临时验证实例一律用 **8090** 并在结束后 `Stop-Process` 清理 java；**8080 留给用户在 IDEA 启动**；`mvn package` 前必须先停应用，否则 repackage 会因 jar 被占用而失败
- 里程碑进展：无新功能开发；为 M5 收尾（部署 + 简历）做准备
- 下一步：见 `handoff.md` 的「下一步」段

## Git 提交记录（S-010 追加）

| 日期 | 提交 | 内容 |
|---|---|---|
| 2026-10-15 | （待用户提交） | docs(plans): 记录业务数据清空与空数据预检（S-009）+ 今日收尾交接（S-010） |

### S-011 | 2026-10-15：会话切换交接（用户切换到新会话，模型不变：DeepSeek V4.1 flash）

- 交接原因：用户即将**切换新会话**继续开发，模型仍为 **DeepSeek V4.1 flash**
- 核对到的实际状态（切换前最后一次确认）：
  - **代码**：全部已提交，HEAD = `9e8f12f`（批次 1-4 依次为 `0f27158`、`4f39c5a`、`4caec14`、`9e8f12f`）
  - **工作区**：仅 3 个项目记忆文档未提交（`handoff.md`、`progress.md`、`task_plan.md`）
  - **环境变化（重要）**：`MySQL80` 服务当前 **Stopped**（StartType = Manual），3306 无监听、无 mysqld 进程 → **新会话开工前必须先以管理员身份启动 MySQL80**（沙箱无权限启动服务；用户在服务管理器或管理员终端执行 `net start MySQL80`）
  - **数据**：MySQL 停止前最后一次核对为「5 张业务表均 0 行；sys_user 5、department 68」——数据已持久化，服务重启后仍在（停止期间无法再核对，故以该值为准）
  - **时间点说明**：本条记录时，会话内日期已跨到 2026-10-15
- 交接文档更新：`handoff.md` 增加「交接原因 + 环境现状（MySQL 已停止）」；`task_plan.md` 的 `Next Step` 首条改为「先启动 MySQL80」
- 新会话恢复方式（用户可直接粘贴给新会话）：
  `继续 it-service-desk 项目（路径 D:\DeepSeek_data\gradaute_space\it-service-desk）。按 project-memory skill（v3）恢复状态：读项目根 AGENTS.md → .dsh/plans/.active_plan → .dsh/plans/it-service-desk/handoff.md → task_plan.md。注意：MySQL80 服务当前是停止的，需要你先确认已启动。`
- 下一步：①启动 MySQL80 ②用户跑完整闭环并反馈 ③确认部署目标后进入 Day 7（部署 + README + 简历）

## Git 提交记录（S-011 追加）

| 日期 | 提交 | 内容 |
|---|---|---|
| 2026-10-15 | （待用户提交） | docs(plans): 会话切换交接（S-011）—— HEAD 状态、环境现状（MySQL 停止）与恢复方式 |

### S-012 | 2026-10-15：用户实跑闭环验证结果（数据核对）

- 背景：用户启动 MySQL80 后实跑了一遍完整闭环，AI 通过数据库 + uploads 目录核对实际结果
- 环境核对：
  - `MySQL80` **Running**，3306 已监听（PID 10784）——注意：`Get-NetTCPConnection` 在本机对 3306/8090 都报 0，**该 cmdlet 在本环境不可靠**，应以 `netstat -ano` / `Test-NetConnection` 为准（本会话曾因此误判）
  - **应用当前未运行**：无任何 java 进程、8080 未监听（用户跑完闭环后已关闭，或尚未再次启动）
- 实跑结果（来自库中数据，时间 2026-10-15 01:15 ~ 01:19）：
  | 步骤 | 证据 | 结论 |
  |---|---|---|
  | ①科室报修 | report id=1 / **R000001** / 急救中心-一层-EICU(56) / 张华 / 打印机 / 缺墨 / `DISPATCHED` | ✅ 编号已从 1 重新开始 |
  | ②管理员派单 | work_order id=1「打印机报修」/ assignee=3(张工) / 01:15:51 | ✅ |
  | ③维修完成 | work_order `status=COMPLETED`、`repair_record=碳粉1支`；consumable 碳粉×1 | ✅ |
  | ④科室签字（**代签**） | `signed_by=方彦博(由张华代签)`、`signature_url=/uploads/09b525d5….png`（01:17:37，文件确实存在 11,448 字节） | ✅ 代签 + 签名留痕 |
  | ⑤管理员核价 | consumable `unit_price=40.00` | ✅ |
  | ⑥生成月度报销单 | reimbursement id=1 / 2026-10 / dept 56 / **¥40.00** / `PENDING_REIMBURSE`；reimbursement_item 关联工单#1 | ✅ 金额与核价一致 |
  | ⑦科室签字报销单 | `signed_by=方彦博`、`signed_at=01:19:01`、`signature_url=/uploads/e178da1d….png`（文件存在 11,428 字节） | ✅ **D-009 修复项在真实使用中生效**（原先签名会丢） |
  | ⑧财务回填 | `reimbursed_at=NULL` | ⏳ **尚未执行（最后一步）** |
- 意义：M5 的「签字 + 报销闭环」在修复后首次由用户实跑验证通过（报销单签名留痕、代签、核价均为真实数据证据）
- 遗留：工作区 3 个记忆文档仍待用户提交；`uploads/` 现有 8 个文件（6 个历史孤儿 + 2 个本次有效签名）
- 下一步：用户启动应用 → 完成第 ⑧ 步「财务回填」→ 工作台核对指标；随后 Day 7

## Git 提交记录（S-012 追加）

| 日期 | 提交 | 内容 |
|---|---|---|
| 2026-10-15 | （无需提交） | 仅为数据核对记录，无代码/文档变更 |

### S-013 | 2026-10-15：闭环最后一步「财务回填」已执行并全量核对（**闭环 100% 跑通**）

- 背景：用户要求 AI 用临时实例（8090）把第 ⑧ 步「财务回填」跑掉并核对界面与指标
- 执行动作（**数据被真实修改，属用户明确要求的验证**）：
  - `POST /api/reimbursements/1/reimburse`（token-1）→ 报销单 #1 由 `PENDING_REIMBURSE` 变为 **`REIMBURSED`**，`reimbursed_at = 2026-10-15 15:14:29`
  - 验证脚本：`.rdoc_inspect/verify-reimburse.js`（真实后端 + jsdom 界面）
- 验证结果 → **ALL CHECKS PASSED（24 项）**：
  - 回填前：报销单#1 待报销、本月金额 ¥40.00、**闭环中间态清零**（待受理 0 / 进行中 0 / 待验收 0）
  - 回填后：待报销 1→0、已报销 0→1；详情 `REIMBURSED` + **保留签字留痕**（`/uploads/e178da1d….png`）+ 关联工单 1 条
  - 权限复核：李工查张工工单 **403**、张工查本人 **200**、维修工查报销单 **403**（D-009 修复项在真实数据上生效）
  - 界面核对：工作台四卡（今日新增 1 / 待受理 0 / 进行中 0 / 待验收 0）、报销概览显示 **¥40.00 与已报销 1**、侧栏待签字角标隐藏；报销页状态「已报销」且**不再显示「财务回填」按钮**；详情抽屉出现「报销时间」并**展示手写签名图**；无未捕获脚本错误
- 意义：**M5 的「签字 + 报销闭环」全流程（报修→派单→维修+耗材→科室签字[代签]→核价→月报销→科室签字→财务回填）已在修复后由用户实跑 + AI 核对，100% 通过**，且每一步都有数据库/文件/界面三层证据
- 环境：临时实例已关闭（无残留 java 进程），8080 留给用户在 IDEA 使用
- 数据现状：report=1、work_order=1、reimbursement=1（一条完整闭环的演示数据，可继续用于演示/截图）
- 下一步：Day 7 部署 + README + 简历（部署目标待用户确认）

## Git 提交记录（S-013 追加）

| 日期 | 提交 | 内容 |
|---|---|---|
| 2026-10-15 | （无需提交） | 仅数据与验证操作：财务回填 + 闭环核对（代码/文档无变更） |

### S-014 | 2026-10-15：签字体验改造（签名板重写 + 输入姓名生成 + 签字方式留痕）

- 需求来源：用户反馈「科室签字用鼠标不好操作、不好演示」→ AI 先诊断缺陷再给方案 → 用户选定方案（详见 `findings.md#D-010`）
- 完成：
  - **新建共用组件**：`static/js/signature-pad.js`（约 250 行）+ `static/css/signature-pad.css`；修复 3 个缺陷：画布像素=显示尺寸×DPR（setTransform 对齐 CSS 像素）、线宽 3+圆头圆角、逐笔撤销/贝塞尔平滑/CSS 引导层/导出自动裁剪
  - **双模式**：手写（默认）+ 输入姓名生成（楷体斜体自适应字号），同一画布与上传链路
  - **签字方式留痕**：`model/SignType` 枚举（容错 `of()`）+ `work_order.sign_type` / `reimbursement.sign_type` + 两个 DTO 加 `signType` + 服务/控制器透传；科室端两个签字入口带 `signType`，科室端详情与后台两处详情展示签字方式徽标
  - `report.html`：签字弹窗重构（模式标签页 + 画布 + 撤销/清除 + 姓名输入 + 提示），删除旧 `<canvas width=780>` 死代码与 `.canvas-box` 样式
  - **首个后端单元测试** `src/test/java/com/itservice/desk/model/SignTypeTest.java`；系统信息页「无后端单测」限制改写
  - 迁移脚本 `sql/2026-10-15-sign-type.sql`（幂等，已执行）；SW `itsd-v5 → itsd-v6` 并纳入 2 个新资源
- 验证（证据）：
  - **组件专项** `test-signature-pad.js`（jsdom + 记录型 2D 上下文）→ **22 项全通过**：画布 400×170（dpr1）/800×340（dpr2）+ `setTransform` 正确；**点可视中心落笔即 (200,85)**（证明坐标不再挤压到左上角）；线宽 3、圆头圆角、≥3 点走贝塞尔；撤销/单击圆点；`contentBox` 精确框出墨迹 `{100,40,51,21}`；导出裁剪为 **79×49** 且起点含 14px 留白；TYPED 模式 `fillText` 传入姓名、居中 (200,85)、`italic` + `KaiTi` 字体
  - **端到端** `test-sign-type.js`（真实后端，跑完**自动 SQL 清理**）→ **11 项全通过**：工单签字 `sign_type` 落库=TYPED、科室端与后台接口均返回、报销单签字同样落库、历史 NULL 值不报错
  - **回归**：`test-frontend.js`（新增签名板接入/双模式/带 signType/展示签字方式 4 项断言 + 「签字板交互」小节 9 项）**全通过**；`test-admin-app.js`（新增工单与报销单「签字方式」断言）**全通过**
  - **后端单测**：`mvn test` → **Tests run: 6, Failures: 0, Errors: 0**（首次跑需联网拉 `surefire-junit-platform:2.22.2`，已入 `.m2repo`）
  - HTTP：`/js/signature-pad.js`(application/javascript)、`/css/signature-pad.css`(text/css) 200；SW 已 `itsd-v6`
  - 构建：`mvn package` → BUILD SUCCESS（含测试）
- 错误：
  - 测试桩把计数器与方法同名（`stroke`）导致 `ctx.stroke` 被自增成数字 → 改名 `strokeCount`
  - 组件测试断言写错两处：文字用的是 CSS 逻辑坐标（应为 200,85 而非 800 画布中心）；裁剪发生在**离屏画布**的新上下文（不该在源画布上下文找 `drawImage`）
  - `SignTypeTest` 引用了未实现的 `PHOTO` → 编译失败；按"只保留实际支持范围"删除该断言，并同步移除前端 `SIGN_TYPE` 映射里的 PHOTO 死配置
  - `test-admin-app.js` 三处断言不兼容零值（角标为 0 时元素隐藏、空态占位行被当数据行、待受理为空时无派单按钮）→ 改为兼容「有数据/空数据」两种状态
- 里程碑进展：M5 收尾前的体验改造（用户实跑闭环后的反馈项）
- 下一步：用户复验签字体验；随后 Day 7 部署 + README + 简历

## Git 提交记录（S-014 追加）

| 日期 | 提交 | 内容 |
|---|---|---|
| 2026-10-15 | （待用户提交） | feat(sign): 签名板重写（画布/线宽/撤销/自动裁剪）+ 输入姓名生成 + sign_type 留痕 + 首个后端单测 |

### S-015 | 2026-10-15：权限体系落地（A 档 5 项：动态权限 + 财务角色 + 文案/过滤 + 关系显式化 + 报销进度）

- 需求来源：用户提供《设计重构.md》参考并强调「不要过度设计、挂起/驳回/取消 后续版本再加」→ AI 逐条筛选出 A 档 → 用户「A 档全做」（决策与权限矩阵见 `findings.md#D-011`）
- 完成：
  - **A1 权限单一事实来源**：新增 `service/WorkOrderAccess`（`canView` 行级 + `allowedActions` 状态机动态）；`WorkOrderController` 的 6 个入口全部改走该类；详情接口改为 `WorkOrderDetail{workOrder, allowedActions, reportNo, reimbursement}`
  - **A2 FINANCE 角色**：`UserRole.FINANCE` + 账号 `finance/123456`；报销单四个接口放行财务（`isReimbursementStaff`）；后台菜单 `visibleMenu()` 按角色渲染、`defaultPath()` 财务落地报销单；`today.html` 角色兜底补 FINANCE；`login.html` 改为按角色直达（报修人→create、维修工→today、其余→admin）
  - **A3 文案统一 + 范围过滤**：五处状态映射统一为 待受理/待接单/维修中/待签字/已完工；工单管理默认「进行中」，下拉含 全部/单状态
  - **A4 关系显式化**：详情返回服务端 `reportNo`；待受理页副标题说明派单后转工单
  - **A5 报销进度**：详情抽屉新增「报销进度」行（按 reimbursement_item 反查）
  - 前端：`ui.js` 新增 `ACTION_LABEL/actionLabel/can` 与签字方式徽标；工单详情核价按钮与输入框按 `allowedActions` 的 PRICE 渲染；`store.js` 拆出 `roleCode/isBackendUser/canViewStats`
  - 迁移：`sql/2026-10-15-finance-role.sql`（幂等，已执行）；SW `itsd-v6 → itsd-v7`
- 验证（证据）：
  - **后端单测 17 项全通过**：`SignTypeTest` 6 + 新增 `WorkOrderAccessTest` 11（管理员/调度员全量+PRICE、维修工三态 ACCEPT/REPAIR/收窄只读、非本人不可见、科室端 SIGN 与其它状态仅 VIEW、跨科室拒绝、报修人按报修单归属、财务不接触工单、匿名拒绝）
  - **财务权限边界** `test-finance-api.js` **16 项全通过**：财务可登录/可看报销单/可回填（已报销单据被业务状态拦下而非权限拦下）；工单列表·工单详情·今日任务·报修单池·用户·系统信息·概览统计**全部 403**；管理员权限回归正常、登录响应不含密码
  - **签字 + 动态权限** `test-sign-type.js` **15 项全通过**：待签字阶段科室端动作集 `[VIEW,SIGN]`、维修工收窄 `[VIEW]`、签字后科室端 `[VIEW]`；管理员 `[VIEW,PRICE]`；`reportNo` 与报销进度字段随详情返回
  - **UI 回归**：`test-admin-app.js`（新增 工单默认「进行中」不含已完工、范围 6 选项、报销进度行、核价按钮可见、**财务角色 8 项**：仅 1 个菜单分组/仅月度报销单/默认落地 `#/reimbursements`/无角标请求/无脚本错误）**全通过**；`test-frontend.js`（新增状态文案统一断言）**全通过**；`test-signature-pad.js` 22 项**全通过**
  - **接口 E2E** `test-batch4-api.js` 24 项**全通过**（覆盖改造后的 accept/repair/pricing/sign 与详情归属校验）
  - 数据基线：验收后清理测试数据，5 张业务表恢复为各 1 条（`report/work_order/consumable/reimbursement/reimbursement_item`，max_id=1），`sys_user=6`、`department=68`
- 错误：
  - **`/api/work-orders/today` 缺角色校验**：财务调用返回 200 空列表（虽无数据泄露，但权限模型不一致）→ 补「仅维修人员可查看」，由 `test-finance-api.js` 抓出
  - `SignTypeTest` 编译失败（引用未实现的 `PHOTO`）→ 按实际支持范围删断言并同步移除前端死配置
  - `test-admin-app.js` 三处断言不兼容数据状态（角标为 0 时元素隐藏、空态占位行被当数据行、待受理为空时无派单按钮）→ 改为兼容「有数据/空数据」
  - `test-batch4-api.js` **不可重跑**（3 分钟内相同报修被防重口径拦截）→ 描述加时间戳，使脚本可反复执行
  - 进程管理踩坑：`Start-Process -WindowStyle Hidden` 启动的实例未生效（端口被前台诊断实例占用），前台 `java ... | Select-Object -First N` 会留下存活进程 → 统一改用**后台作业**启动 + 轮询探活
  - `verify-reimburse.js` 是**一次性状态依赖脚本**（需要存在待报销的报销单），在已全部报销的数据上必然失败；其 24 项结果已记录于 `#S-013`，不作为常规回归
- 里程碑进展：M5 的权限体系与状态机动态权限完成（简历可讲：RBAC + 行级 + 字段可见性 + 状态机动态权限 + 财务剥离）
- 下一步：用户复验后台（尤其财务账号）；随后 Day 7 部署 + README + 简历

## Git 提交记录（S-015 追加）

| 日期 | 提交 | 内容 |
|---|---|---|
| 2026-10-15 | （待用户提交） | feat(auth): WorkOrderAccess 统一行级+状态机动态权限、allowedActions 下发、FINANCE 财务角色、状态文案统一、工单默认进行中、详情显示报销进度 |

### S-016 | 2026-10-15：用户复验 A 档后的反馈（跨端状态同步未实现）— **暂停推进**

- 用户原话（逐字）：> "完全错了，状态还是没有同步上。可能是是触发了上下文压缩。这样，你先保留项目进度，我切换对话进行项目推进。"
- 处理：**不猜测、不改代码**，按用户要求先把进度落盘（handoff 重写 + `findings.md#D-012`），用户将切换会话继续
- 已核实并写入 `#D-012` 的事实：全仓库无 SSE/WebSocket/visibilitychange；维修端不刷新看不到新派单、科室端不刷新看不到待签字、后台角标不自更新 → 三端之间零同步
- 我的取舍失误（诚实记录）：参考文档「实时同步机制」要求 SSE/WebSocket 而非轮询，我在 A 档把同步整体划到「不做 / B 档可选」，用户批准的只有 A 档 → 同步一行未做；用户"完全错了"的感受来源很可能是**最直观的跨端同状态**，而不是我交付的权限矩阵
- 下一步（下一会话第一件事）：先向用户确认复现场景（跨端同步 vs 某端文案不一致），再选「焦点自动刷新（小）」或「SSE 单向推送（约 1 批次）」
- 代码现状：A 档 38 个文件已由用户提交（提交 `472c30e`，提交信息与 AI 建议一致）；工作区仅剩 4 个记忆文档未提交；已通过 17 项后端单测 + 前端/接口验收全绿；数据基线已恢复（5 张业务表各 1 条）；8090 实例已停（无 java 进程）

### S-017 | 2026-10-15：工单业务管理重构——报修单状态同步 + 科室端历史整合

- 完成：
  - 诊断（实地核查代码 + 数据库）：报修单派单后 status 永远停在 DISPATCHED（`dispatch()` 写入后无后续回写），工单已 COMPLETED 也不反映 → 报修历史看不到"已完成"；科室端 4 张分散卡片设计混乱
  - 4 项最小改动（不新建表、不改数据模型，详见 `findings.md#D-013`）：
    - A：`ReportStatus` 去 ACCEPTED 加 COMPLETED；`WorkOrderService.sign()` 回写 report.status=COMPLETED
    - B：report.html 删独立「已完成工单」卡片，并入「本科室报修与维修历史」单一列表（报修单为主轴，按 reportId 关联工单号/签字人/完成时间）；签字成功后刷新历史（原漏调 loadHistory）
    - C：ui.js/reports-history.js/system.js 状态映射去 ACCEPTED 加 COMPLETED + 状态机图补终态
    - D：迁移脚本 `sql/2026-10-15-report-status-sync.sql` 幂等回填存量，已执行（R000001/R000013 → COMPLETED）
    - SW 缓存升版 itsd-v7 → itsd-v8
  - 验证（8090 临时实例）：
    - 闭环实跑：报修(R000014 PENDING)→派单(DISPATCHED/ASSIGNED)→接单(IN_PROGRESS)→维修(PENDING_ACCEPT)→签字(wo=COMPLETED+SIGNED)→**查库 report.status=COMPLETED ✓**
    - 接口核对：`/api/reports/by-department` 与 `/api/reports` 返回 COMPLETED；`/api/work-orders/by-department` 带工单号+签字人
    - `mvn -Dmaven.repo.local=.m2repo test` 17 项全过
    - 前端 jsdom：test-frontend.js / test-admin-app.js / test-sign-type.js / test-finance-api.js 全部 ALL CHECKS PASSED
    - 测试造数据已清理，基线恢复：report 2、work_order 2、consumable 1、reimbursement 1
- 结果/输出：用户诉求「工单状态同步」+「历史工单最后状态=已完成」+「设计混乱」三项均已解决；D-012 的跨端实时推送部分仍未做（不在本次范围）
- 错误：mvn test 首次因 .m2repo 缺 surefire-junit-platform + 沙箱无权限写 D:\Maven\repository → 用 `-Dmaven.repo.local=.m2repo`（在线拉取到项目内仓库）解决
- 代码状态：**待用户提交**（HEAD 仍为 `472c30e`）；本次改 7 个文件 + 新增 1 个 sql，工作区另有记忆文档更新
- 环境现状：8090 实例已停，无 java 进程；MySQL80 Running；8080 留给 IDEA
- 下一步：用户复验科室端报修历史列表（应显示「已完成」+ 工单号/签字人）；之后 D-012 跨端实时推送（焦点自动刷新 或 SSE）或 Day 7 部署/README/简历

## Git 提交记录（S-017 待提交）

| 日期 | 提交 | 内容 |
|---|---|---|
| 2026-10-15 | （待提交） | fix(work-order): 报修单状态随工单流转同步回写 COMPLETED + 科室端历史列表整合 + 状态映射统一 |

### S-018 | 2026-10-15：报销闭环同步——财务回填后报修单回写「已报销」

- 完成（详见 `findings.md#D-014`）：
  - `ReportStatus` 加 `REIMBURSED`；`ReimbursementService.reimburse()` 回填后按明细反查工单回写 report.status=REIMBURSED（只推进 COMPLETED，幂等）
  - 前端 4 处映射/展示同步（ui.js / reports-history.js / system.js / report.html，已报销用紫色区分）
  - 迁移脚本 `sql/2026-10-15-report-reimbursed-sync.sql` 已执行（存量 R000001 → REIMBURSED）
  - SW itsd-v8 → itsd-v9
- 验证（8090 端到端）：生成报销单#9→签字→财务回填→R000013/R000016 均变 REIMBURSED ✓；mvn test 通过；前端三套回归 ALL CHECKS PASSED；测试数据已清理恢复基线
- 错误：无
- 代码状态：**待用户提交**（连同 S-017 改动一起）
- 下一步：用户复验（后台「报修单历史」应显示已报销徽标；科室端历史列表紫色「已报销」）→ git 提交 → D-012 实时推送 或 Day 7

## Git 提交记录（S-017/S-018 待提交）

| 日期 | 提交 | 内容 |
|---|---|---|
| 2026-10-15 | （待提交） | feat(reimbursement): 财务回填后报修单同步回写「已报销」+ 存量迁移 |

### S-019 | 2026-10-15：工单状态机重构——严格对齐《工单全流程规则.md》6 态

- 完成（详见 `findings.md#D-015`）：
  - 设计阶段：先读规则文件 → 用 ask_user_question 确认 4 个关键决策点（6态/核价独立/签字→待报销移入历史/报修单粗粒度同步）+ 方案A（工单管理下拉覆盖，不新建历史视图）→ 用户选定方案A
  - 工单状态机重构为严格 6 态：待接单/维修中/待核价/待签字/待报销/已报销
  - 核价提升为状态机正式一环（pricing 接口语义变更：核价完成推进状态）
  - 签字后→待报销（移入历史），月度报销→已报销
  - 报修单粗粒度同步：待报销→已完成，已报销→已报销
  - 前端 6 处状态映射 + 工单管理下拉 8 选项 + 报销按钮 + dashboard 指标 + 单测同步
  - 迁移脚本已执行（存量工单#1→REIMBURSED、#7/#11/#12→PENDING_REIMBURSE）
  - SW itsd-v9 → itsd-v10
- 验证（8090 实跑闭环 7 步全绿）：报修→派单→接单→维修提交(待核价)→核价(待签字)→签字(待报销)→月度报销(已报销)，每步查库工单+报修单状态正确；mvn test(12项)+前端四套回归 ALL PASSED
- 错误：test-admin-app/test-sign-type 首次失败因断言硬编码旧状态(PENDING_ACCEPT/COMPLETED/6选项/待验收)，已更新断言为新状态机
- 代码状态：**待用户提交**（连同 D-013/D-014 改动）
- 下一步：用户复验（工单管理默认进行中4态、切待报销看历史、详情报销按钮、系统信息页6态状态机图）→ git 提交 → D-012 实时推送 或 Day 7

## Git 提交记录（D-013/D-014/D-015 待提交）

| 日期 | 提交 | 内容 |
|---|---|---|
| 2026-10-15 | （待提交） | feat(work-order): 工单状态机重构为6态(待核价/待报销/已报销)+核价独立环节+签字移入历史+报修单粗粒度同步 |

### S-020 | 2026-10-15：科室索引管理修复——搜索失焦 + 布局重排

- 完成（详见 `findings.md#D-016`）：
  - 根因1：搜索 onInput 重建整卡（含搜索框）→ 失焦；根因2：`.dept-*` 五个 CSS 类从未定义 → 竖列堆叠
  - 修复：搜索框 render 时创建一次、输入只重绘数据区；补齐院区三栏 grid + 楼栋分区 + 楼层行流式 chips 样式（~90 行 CSS）
  - `departments.js` 裸 `DeptCascade` → `window.DeptCascade`；test-frontend 状态映射断言同步 6 态（上轮遗留）；SW itsd-v11
  - 新增专项测试 test-departments-view.js（15 项全过）
- 附带发现：department_head 全空（68/68）→ 负责人搜索无数据可搜（逻辑正确），需后续维护数据
- 验证：专项 15 项 + test-frontend + test-admin-app 全部 ALL PASSED
- 错误：无（测试脚本 fetch 相对路径走了 jsdom base、断言 parseInt 全角括号两处自纠）
- 代码状态：**待用户提交**（连同 D-013/D-014/D-015 改动）
- 下一步：用户复验科室索引页（搜索连续输入 + 三栏布局）→ git 提交 → D-012 实时推送 或 Day 7

## Git 提交记录（D-013~D-016 待提交）

| 日期 | 提交 | 内容 |
|---|---|---|
| 2026-10-15 | （待提交） | fix(departments): 科室索引搜索不失焦 + 院区三栏布局重排；工单状态机6态重构(D-015)一并提交 |

### S-021 | 2026-10-15：科室索引搜索功能移除（用户裁定冗余）

- 完成（详见 `findings.md#D-017`）：重写 departments.js 去掉搜索框/过滤逻辑/「当前筛选结果」卡（改为「当前展示」），保留刷新按钮；专项测试 9 项 + test-admin-app 全部 ALL PASSED；SW itsd-v12
- 错误：无（8090 探活遇 Clash 代理拦截用 -NoProxy 绕过；测试 onclick 断言改 dispatchEvent）
- 代码状态：**待用户提交**（连同 D-013~D-016 改动）
- 下一步：用户复验 → git 提交 → D-012 实时推送 或 Day 7

### S-022 | 2026-10-15：签字弹窗费用明细（核价后费用透明）+ 数据库清空配合全流程验证

- 完成（详见 `findings.md#D-018`）：
  - 按用户要求清空 5 张业务表（先备份 business-data-20261015-214245.sql），重置自增；保留 68 科室 + 6 账号
  - 科室端签字弹窗新增费用明细区：耗材×数量×核价单价+合计；未核价标红提示；无耗材隐藏
  - SW itsd-v12 → itsd-v13；新增专项测试 test-sign-cost.js（13 项 ALL PASSED，真实后端数据流）
- 错误：①测试脚本 headers 覆盖丢 Content-Type 致耗材没写库（合并修复）②**误杀用户 IDEA 8080 实例**（按启动时间过滤进程太粗，应按端口定位）→ 已提醒用户重启
- 数据状态：用户全流程验证中——工单#1（用户 22:09 造，碳粉 ¥40 已核价，待签字）；我的测试数据已清
- 代码状态：**待用户提交**（连同 D-013~D-017 改动）
- 下一步：用户重启 IDEA 后继续全流程验证（签字时可见费用明细）→ git 提交

### S-023 | 2026-10-15：UI 文案 + 详情费用 + 数据留存策略

- 完成（详见 `findings.md#D-019`）：
  - 文案：report.html 标题「本科室报修与维修历史」→「报修记录」
  - 详情弹窗费用：openHistoryDetail 加「报修费用」区（耗材×数量×核价单价+小计+合计，未核价标红）
  - 报修留存：ReportService RETENTION_DAYS=60，listReportsByDepartment/listReports 加 60 天过滤
  - 报销留存：ReimbursementService RETENTION_DAYS=180，listAll/listByDepartment 加 180 天过滤
  - 前端提示：报修卡片「仅展示近 2 个月」+ reimbursements「仅展示近 6 个月」
  - SW itsd-v13 → itsd-v14
- 验证：造 61 天前报修单 → 接口过滤生效（库内未删）✓；工单#1 费用明细 ¥40.00 ✓；mvn test + 前端回归 + 专项 10 项全过
- 取舍：查询过滤而非定时删除（数据不丢可追溯，符合需求规格"防误删"建议）；不加 is_deleted 字段
- 代码状态：**待用户提交**（连同 D-013~D-018 改动）
- 下一步：用户复验（详情弹窗费用 + 报修记录文案 + 留存过滤）→ git 提交 → D-012 实时推送 或 Day 7

### S-024 | 2026-10-15：前端去 slop 重构（design-taste-frontend skill 走查）

- 完成（详见 `findings.md#D-020`）：
  - 诊断：lint 44 FAIL + inspect 三页截图，确认"AI 味"=28 阴影/12 细线/16 描边/4 渐变/28 emoji/对比度不足
  - 三端同等力度清理：共享基座（admin-layout.css）+ admin 视图层 + 科室端 + 维修端 + login + dept-cascade/signature-pad 组件
  - lint 全量 44→0 FAIL；inspect 对比度类 0 FAIL（剩 few-pictures/still-page 落地页专属要求，内网工具页豁免）
  - mvn test + 前端四套回归全部 ALL PASSED；SW itsd-v15
- 错误：mvn clean 多次失败（jar 被占用）→ 停 java 后等待不够；改 mvn package（不 clean）+ 等更久解决
- 代码状态：**待用户提交**（连同 D-013~D-019 改动）
- 下一步：用户复验去 slop 后的视觉 → git 提交 → Day 7



### S-025 | 2026-10-16：三端隔离 + PWA 引导 + 入口二维码（PWA 替代 APK 的架构决策）

- 架构决策（详见 indings.md#D-021）：三端形态 = 一套后端 + 三个浏览器入口 + PWA 壳；APK 留作升级路径（Capacitor 包装，触发条件：真后台推送/离线同步/传感器需求）
- 完成：
  - 三端退出与角色隔离：新增 /js/guard.js（端守卫共用脚本）——today/detail（维修端）、create（报修人端）加守卫 + 右上角退出按钮；角色不匹配时提示后强制退出回登录页（不跨端跳转），login.html 是唯一分发点；admin 端提示语统一。科室端 report.html 免登录，退出语义 = 解绑科室（按钮文案改「退出 / 重新绑定科室」）
  - PWA 安装引导：新增 /js/pwa-install.js —— Android 捕获 beforeinstallprompt 一键安装，iOS 手动指引条；可关闭（7 天记忆）；挂到 report/today/detail/login/create
  - 入口二维码：pom 加 ZXing core+javase 3.5.3；新增 QrController（/api/qr，PNG 输出，Host 动态拼 URL，路径白名单防钓鱼跳板，管理员/调度员鉴权）；系统信息页新增「三端入口」卡片（.qr-grid 样式）
  - Service Worker itsd-v16→v18（v17 为通栏布局修复轮）；新文件 guard.js/pwa-install.js 入 APP_SHELL
- Maven 注意：CLI 需 -Dmaven.repo.local=.m2repo 且 JAVA_HOME=D:\DevTools\Java\jdk17（默认 JAVA_HOME 是 jdk8，会报「无效目标发行版 17」）
- 下一步：用户复验（扫码入口 + 退出隔离 + PWA 引导条）→ git 提交 → 第 2 步通知渠道（待定企微 webhook 或短信）
