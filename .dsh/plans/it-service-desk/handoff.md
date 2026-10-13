# 交接摘要：it-service-desk

> 本文件是给下一个会话的交接单。保持精炼，引用而非复制已有内容。

## 当前状态

- 计划：it-service-desk
- 阶段：Day 4 已完成（管理员后台），准备进入 Day 5（维修人员端）
- 里程碑：M0 ✅ / M1 ✅ / M2 ✅ / M3 ✅；M4、M5 pending
- 状态：已保存进度

## 已完成（摘要）

- M0 内存闭环 → Day1 MyBatis-Plus+MySQL → 权限控制 → Day2 报修单/工单分离 → Day3 科室免登录报修端 → Day4 管理员后台
- Day3 修复 4 项：undefined 渲染、表单重置、10 分钟防重复提交、Toast+科室负责人只读
- Day4：GET /api/users（管理员/调度员，?role=REPAIRER 过滤，密码脱敏）+ admin.html（待受理列表/派单弹窗/基础数据）+ 登录路由
- 预置 DEV001 绑定（门诊楼-三层）；department_head 默认负责人

## 关键决策与约束

- 三端：科室报修端（免登录，设备码）/ 管理员后台 / 维修人员端
- 报修单≠工单：科室提交 → 管理员派单 → 工单
- 权限：创建报修仅 REPORTER；免登录凭 device_code；派单/查看报修单/用户列表仅 ADMIN/DISPATCHER；维修仅 REPAIRER
- 10 分钟防重复：同科室同设备 10 分钟内拒绝
- Maven 双仓库：IDEA 用 D:\Maven\repository；沙箱 CLI 用 .m2repo（代理 127.0.0.1:7890）
- 构建：JAVA_HOME=D:\DevTools\Java\jdk17
- 临时目录 .rdoc_inspect 已 gitignore 并移出跟踪

## 最近提交

- 8e7458f feat: Day4 管理员后台
- b075441 fix: 科室报修页四项修复
- 2ce3659 feat: Day3 科室免登录报修端
- 9db2163 feat: Day2 报修单/工单分离
- f2e8706 feat: 权限控制

## 下一步

- Day 5：维修人员端（接单、现场确认、维修/耗材/欠条记录、发起签字）
- 详细见 task_plan.md

## 建议技能

- project-memory：自动恢复项目状态