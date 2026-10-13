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