-- 2026-10-17 消息通知（A 档：站内消息中心 + 前台强提醒 + 超时升级）
-- 说明：一次性迁移脚本。ADD COLUMN 重复执行会报 Duplicate column，已执行过请跳过。
-- 新库无需执行本文件：schema.sql 已包含同样的列与表。

-- 1) 报修单记录「提交设备」，用于把待签字消息只投给报修人那台手机
ALTER TABLE report
    ADD COLUMN client_device_id VARCHAR(64) NULL COMMENT '提交该报修的科室端设备标识';

-- 2) 设备绑定标记「我是本科室负责人（护士长）」，用于定向投递待签字消息
ALTER TABLE department_binding
    ADD COLUMN is_head TINYINT DEFAULT 0 COMMENT '1=该设备为本科室负责人(护士长)设备';

-- 3) 站内消息表
CREATE TABLE IF NOT EXISTS notification (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_key     VARCHAR(120) NOT NULL COMMENT '业务事件幂等键，如 DISPATCH:12',
    target_kind   VARCHAR(10)  NOT NULL COMMENT 'USER=登录用户 / DEVICE=科室端设备',
    target_id     BIGINT       NOT NULL COMMENT 'USER 时为 sys_user.id；DEVICE 时为 department_binding.id',
    type          VARCHAR(40)  NOT NULL COMMENT 'REPORT_CREATED/DISPATCHED/PENDING_SIGN/TIMEOUT_UNACCEPTED',
    title         VARCHAR(200) NOT NULL,
    body          VARCHAR(500),
    level         VARCHAR(10)  DEFAULT 'NORMAL' COMMENT 'NORMAL/URGENT（URGENT 才响铃震动）',
    work_order_id BIGINT,
    report_id     BIGINT,
    created_at    DATETIME,
    read_at       DATETIME,
    UNIQUE KEY uk_event_target (event_key, target_kind, target_id),
    KEY idx_target (target_kind, target_id, id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
