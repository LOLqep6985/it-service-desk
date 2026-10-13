# 交接摘要：it-service-desk

> 本文件是给下一个会话的交接单。保持精炼，引用而非复制已有内容。

## 当前状态

- 计划：it-service-desk
- 阶段：M0 最小验证已通过，准备进入 Day 1（完整数据库设计）
- 里程碑：M0 complete；M1 pending
- 状态：等待用户确认 Day 1 开始 + 解决数据库驱动依赖问题

## 已完成

- M0 垂直切片：Spring Boot 2.7.18 工程 + 内存数据 + 手机端三页面（登录/今日任务/工单详情），工单闭环测试通过
- 构建环境踩坑全部解决（JDK17 / 离线 Maven / lombok 版本）
- 应用在 8080 端口可运行：mvn -o 打包后 java -jar target\desk-0.1.0.jar

## 未完成与阻塞点

- Day 1 完整数据库设计未开始
- **阻塞**：mysql-connector-j 与 MyBatis-Plus 依赖无法下载（Maven 中央仓库/阿里云镜像 HTTP 000）
  - 候选解法：找可用镜像（腾讯/华为/其他）/ 手机热点网络 / 手动下载 jar 并 mvn install:install-file

## 关键约束与决策

- 技术栈：Java 17 / Spring Boot / MyBatis-Plus / MySQL / Redis(可降级) / Vue3
- M0 用内存存储；完整开发阶段必须接 MySQL
- Maven 本地仓库：D:\Maven\repository；构建需 JAVA_HOME=D:\DevTools\Java\jdk17
- 移动端优先 / 双端同后端 / 网络异常处理 / AI 后置（详见 findings.md 与 AGENTS.md）

## 下一步

- 用户确认后进入 Day 1：先解决 MySQL 驱动依赖，再做完整数据库设计（建表 SQL + 实体）
- 详细阶段见 task_plan.md

## 建议技能

- project-memory：自动恢复项目状态
- u1-ocr-parser-pro：如需再次解析文档