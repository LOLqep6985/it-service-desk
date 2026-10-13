# 交接摘要：it-service-desk

> 本文件是给下一个会话的交接单。保持精炼，引用而非复制已有内容。

## 当前状态

- 计划：it-service-desk
- 阶段：Day 6 已完成 3 个子项；待办：月度报销单 + 全流程联调 + Day7 部署/简历
- 里程碑：M0-M4 ✅；M5 进行中
- 状态：已保存进度（用户切换会话）

## 项目全貌（一句话）

医院多院区 IT 报修/维修/报销三端系统：
科室报修端（免登录，工作手机 DEV001）→ 管理员后台派单 → 维修工接单/维修/耗材(名+量) → 待科室验收 → 护士长/主任手写签名(可代签) → 已完成 → 管理员核价。

## 已完成功能

- 三端：report.html（免登录报修/待验收签字/已完成工单详情含签名图）、admin.html（待受理派单/工单管理详情抽屉/核价/15秒刷新）、today.html+detail.html（维修工接单/维修/耗材登记）
- 权限：报修仅 REPORTER；接单/维修仅 REPAIRER 且限本人；派单/查看报修单/工单/用户/核价 仅 ADMIN/DISPATCHER；科室端凭 device_code
- 10 分钟防重复报修；科室负责人默认带出；手写签名图片留痕；uploads 绝对路径
- 状态机：ASSIGNED → IN_PROGRESS → PENDING_ACCEPT（维修工提交）→ COMPLETED（签字后）
- 耗材：维修工只填名称+数量；单价由管理员在工单详情「核价」填写；欠条字段保留未展示

## 关键环境/事实（重要）

- Maven 双仓库：IDEA 用 D:\Maven\repository（用户 settings.xml）；沙箱 CLI 用 .m2repo（代理 127.0.0.1:7890 + -Dmaven.repo.local=.m2repo + JAVA_HOME=D:\DevTools\Java\jdk17）
- MySQL：库 it_service_desk，账号 itsd/itsd123456，MySQL80 服务需先启动
- 上传目录：D:/DeepSeek_data/gradaute_space/it-service-desk/uploads（gitignore），绝对路径 
- 演示：科室设备码 DEV001（绑定科室1 门诊楼-三层）；账号 admin/admin123、repairer1/123456、reporter1/123456
- 沙箱内无法访问 chatgpt.com/直接下 Maven 中央仓库；代理经 Clash 127.0.0.1:7890

## 待办（下次继续）

1. Day 6 收尾：月度报销单（reimbursement 按月汇总已签字工单+耗材金额 → 护士长签字 → 交财务回填）
2. 全流程联调测试（报修→派单→维修→签字→核价→报销）
3. Day 7：部署配置（Docker/Nginx）+ README + 简历项目描述

## 最近提交

- d17ad0e docs: 保存 Day6-3 完成进度
- bef7e2d feat: 工作留痕（签名展示 + uploads 绝对路径）
- 79bdc05 feat: Day6-2 签字闭环 + 核价
- f617d34 feat: Day6-1 工单详情抽屉

## 下一步

- 新会话：读本文件 + task_plan.md 恢复状态；用户说「继续 Day 6」即可

## 建议技能

- project-memory：自动恢复项目状态
- u1-ocr-parser-pro：如需解析文档