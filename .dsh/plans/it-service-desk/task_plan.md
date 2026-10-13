# 任务计划：it-service-desk

## 目标

构建「多院区现场 IT 运维工单与调度系统」：把企业/医院"微信群报修"升级为「PC 管理调度后台 + 手机端现场维修工作台」的移动优先 Web 应用。
- 报修信息集中化 → 维修任务可追踪 → 现场人员快速找到地点和联系人。
- 简历级 Java 全栈项目（Java 17 / Spring Boot / MyBatis-Plus / MySQL / Redis可降级 / Vue3 + Vant/Element Plus）。

## 关键业务约束（追加于 2026-10-13）

- 多维修人员（含用户共 2 人）管理/调度；移动端优先（老院/西院/东院）。
- 双端同后端：PC 管理后台 + 手机 H5 共用同一套后端/数据库/权限。
- 第一版闭环：创建工单 → 手机今日任务 → 查看地点/联系科室负责人 → 提交维修记录并关闭工单。
- 网络异常：超时提示、失败重试、防重复提交、维修记录草稿（不做离线同步）。
- 统计看板/二维码/AI 辅助 → 后置扩展。
- 技术选型服务业务问题，不为显高级而加技术。

## 开发策略

先做最小验证（M0 垂直切片），跑通核心闭环判断可行性；再展开 7 天完整开发。

## 里程碑

| 里程碑 | 目标 | 完成标准 | 状态 |
|---|---|---|---|
| M0 | 最小验证（垂直切片） | 手机端跑通「查看任务→找到报修人→处理并关闭工单」闭环 | **complete（2026-10-13 通过）** |
| M1 | 项目骨架 + 数据库设计 | git 仓库 + Spring Boot 工程 + 完整建表 SQL | pending |
| M2 | 用户权限 + 后端基础 | JWT 登录 + RBAC（管理员/调度/维修工/报修人）+ 统一返回/异常 | pending |
| M3 | 工单核心 + 设备资产 | 创建/派单/处理/关闭/评价 + 状态流转 + 资产 CRUD | pending |
| M4 | 移动端 H5 + PC 管理后台 | Vant 三核心页面 + Element Plus 后台 + 网络异常处理 | pending |
| M5 | 联调测试 + 部署 + 简历 | Mockito 测试 + OpenAPI + Docker Compose + 简历描述 | pending |

## 7 天开发计划（M0 已通过）

| 天 | 内容 | 对应里程碑 | 状态 |
|---|---|---|---|
| Day 1 | 项目骨架 + 完整数据库设计 + Git 初始化 | M1 | pending |
| Day 2 | 后端基础：统一返回/异常处理/JWT 登录 + RBAC | M2 | pending |
| Day 3 | 工单核心模块 + 设备资产管理 | M3 | pending |
| Day 4 | 移动端 H5（Vant）：今日任务/工单详情/维修记录 + 网络异常处理 | M4 | pending |
| Day 5 | PC 管理后台（Element Plus）：工单总览/院区科室维护/人员分配/统计看板 | M4 | pending |
| Day 6 | 前后端联调 + Mockito/MockMvc 单元测试 + OpenAPI 文档 | M5 | pending |
| Day 7 | 部署配置（Docker Compose/Nginx）+ README + 简历项目描述 | M5 | pending |

## 当前阶段

M0 已完成并验证通过；准备进入 Day 1（完整数据库设计）。

## Next Step

Day 1：接入真实 MySQL（等用户完成 MySQL 服务启动 + 建库），pom 添加 MyBatis-Plus + MySQL 驱动，完成建表 SQL 与实体映射。

## 已完成

- [x] 项目定义与决策确认（2026-10-13）
- [x] 解析追加对话，纳入业务约束（2026-10-13）
- [x] M0 最小验证：Spring Boot 空壳 + 内存数据 + 手机端三页面 + 工单闭环测试通过（2026-10-13）
  - 已打通：登录→创建工单→今日任务→详情→提交维修→关闭
  - 技术验证：Spring Boot 2.7.18 离线构建成功（JDK17），静态 H5 页面可访问

## 待办/阻塞

- 已解决：Clash 代理（127.0.0.1:7890）+ 项目内本地仓库（.m2repo）可正常从阿里云下载依赖（含 MySQL 驱动、MyBatis-Plus）
- 已解决：mybatis-plus-boot-starter 3.5.7 已下载到 .m2repo
- 备注：沙箱内构建需代理环境变量 + -Dmaven.repo.local=.m2repo；JAVA_HOME 指向 D:\DevTools\Java\jdk17