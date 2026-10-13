# 交接摘要：it-service-desk

> 本文件是给下一个会话的交接单。保持精炼，引用而非复制已有内容。

## 当前状态

- 计划：it-service-desk
- 阶段：Day 1-6 全部完成；待办：Day 7 部署/README/简历 + 最终演示
- 里程碑：M0-M4 ✅；M5 签字+报销闭环 ✅，待「部署+简历」收尾
- 状态：全流程（报修→派单→维修→签字→核价→报销）已联调通过

## 项目全貌（一句话）

医院多院区 IT 报修/维修/报销三端系统：
科室报修端（免登录，工作手机 DEV001/DEV002）→ 管理员后台派单 → 维修工接单/维修/耗材(名+量) → 待科室验收 → 护士长/主任手写签名(可代签) → 已完成 → 管理员核价 → 按月生成报销单 → 护士长签字 → 财务回填。

## 已完成功能

- 三端：report.html（免登录报修/待验收签字/已完成工单详情/月度报销单签字）、admin.html（待受理派单/工单管理详情抽屉/核价/报销单管理/15秒刷新）、today.html+detail.html（维修工接单/维修/耗材登记）
- 权限：报修仅 REPORTER；接单/维修仅 REPAIRER 且限本人；派单/查看/核价/报销单管理 仅 ADMIN/DISPATCHER；科室端凭 device_code
- 状态机：ASSIGNED → IN_PROGRESS → PENDING_ACCEPT（维修工提交）→ COMPLETED（签字后）；报销单：PENDING_SIGN → PENDING_REIMBURSE → REIMBURSED
- 10 分钟防重复报修；科室负责人默认带出；手写签名图片留痕；uploads 绝对路径
- 月度报销：按月汇总已签字工单+耗材金额（reimbursement + reimbursement_item），同科室同月幂等

## 关键环境/事实（重要）

- Maven 双仓库：IDEA 用 D:\Maven\repository（用户 settings.xml）；沙箱 CLI 用 .m2repo（代理 127.0.0.1:7890 + -Dmaven.repo.local=.m2repo + JAVA_HOME=D:\DevTools\Java\jdk17；-D 参数需加引号）
- MySQL：库 it_service_desk，账号 itsd/itsd123456，MySQL80 服务需先启动
- 上传目录：D:/DeepSeek_data/gradaute_space/it-service-desk/uploads（gitignore），绝对路径
- 演示：科室设备码 DEV001（科室1 门诊楼-三层）；账号 admin/admin123、repairer1/123456、repairer2/123456、reporter1/123456
- 沙箱内无法访问 chatgpt.com/直接下 Maven 中央仓库；代理经 Clash 127.0.0.1:7890

## 待办（下次继续）

1. Day 7：部署配置（Docker/Nginx + application.yml 环境变量化）+ README（项目介绍/架构/快速启动/演示账号）+ 简历项目描述
2. 可选体验优化：科室端列表分页、报销单 PDF/打印导出、基础数据管理（科室/用户增删改）
3. 最终全流程演示与截图存档

## 最近提交

- 7fe5e3f docs: 保存当前工作进度（切换会话前交接）
- d17ad0e docs: 保存 Day6-3 完成进度
- bef7e2d feat: 工作留痕（签名展示 + uploads 绝对路径）
- 79bdc05 feat: Day6-2 签字闭环 + 核价

## 下一步

- 新会话：读本文件 + task_plan.md 恢复状态；用户说「继续 Day 7」即可

## 建议技能

- project-memory：自动恢复项目状态
- u1-ocr-parser-pro：如需解析文档