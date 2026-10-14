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