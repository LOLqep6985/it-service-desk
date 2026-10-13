# 交接摘要：it-service-desk

> 本文件是给下一个会话的交接单。保持精炼，引用而非复制已有内容。

## 当前状态

- 计划：it-service-desk
- 阶段：Day 2 已完成（报修单/工单分离数据模型），准备进入 Day 3（科室报修端免登录）
- 里程碑：M0 ✅ complete；M1 ✅ complete；M2-M5 pending
- 状态：已保存进度（用户要求每步保存）

## 已完成（按时间）

- M0 最小验证（内存版闭环）✅
- Day 1：MyBatis-Plus + MySQL 真实持久层，schema.sql/data.sql 自动建表+种子数据，闭环落库 ✅
- 修复：data.sql 中文乱码（sql.init.encoding=UTF-8）；IDEA 启动端口占用（残留 java）✅
- 新增：科室列表 API、报修页、登录按角色跳转、报修按钮仅报修人可见 ✅
- 权限控制：所有工单/报修接口需 X-Token；创建报修仅 REPORTER、提交维修仅 REPAIRER、派单/看报修单仅 ADMIN/DISPATCHER ✅
- Day 2：报修单/工单分离数据模型（report/department_binding/consumable/reimbursement 表 + 实体/Mapper/Service/Controller + 极简报修单表单）✅
  - 验证通过：报修人提交 R000001 → 管理员派单生成工单(report_id) → 维修工处理完成 → 落库

## 未完成与阻塞点

- Day 3（科室报修端免登录）未开始：department_binding 设备码识别 + 照片上传 + 报修单查看/待签字
- Day 4（管理员后台）：待受理报修单列表、派单界面、基础数据管理
- Day 5（维修人员端）：接单、现场确认、耗材/欠条记录、发起签字
- Day 6（签字 + 月度报销闭环 + 联调）
- Day 7（部署 + README + 简历描述）
- 无阻塞

## 关键约束与决策

- 三端：科室报修端（免登录）/ 管理员后台 / 维修人员端
- 报修单 ≠ 工单：科室提交报修单 → 管理员派单 → 工单
- 科室字段极简：科室自动带出 + 报修人姓名 + 设备名称 + 照片 + 简单描述；不填具体地点/优先级
- 签字闭环：维修完成需护士长/指定人签字；月度报销单需护士长签字
- 权限：创建报修仅 REPORTER、维修仅 REPAIRER、派单/查看报修单仅 ADMIN/DISPATCHER
- Maven 双仓库：IDEA 用 D:\Maven\repository；沙箱 CLI 用 .m2repo（代理 127.0.0.1:7890 + -Dmaven.repo.local=.m2repo）
- 构建：JAVA_HOME=D:\DevTools\Java\jdk17

## Git 提交记录（最近）

- 9db2163 feat: Day2 报修单/工单分离数据模型
- f2e8706 feat: 报修/维修接口后端权限控制
- 00cd214 docs: 按三端流程重写路线图
- 24af806 fix: 报修按钮仅报修人可见
- 498c6b0 docs: 纳入三端系统流程
- 7d3e5e3 fix: data.sql 中文乱码
- 6e1e239 feat: Day1 MyBatis-Plus+MySQL

## 下一步

- 用户确认后执行 Day 3：科室报修端免登录（department_binding + 极简表单 + 照片上传）
- 详细阶段见 task_plan.md

## 建议技能

- project-memory：自动恢复项目状态
- u1-ocr-parser-pro：如需再次解析文档