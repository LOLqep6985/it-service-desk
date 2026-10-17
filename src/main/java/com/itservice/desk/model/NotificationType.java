package com.itservice.desk.model;

/** 站内消息类型（A 档只做 4 类事件，见 findings.md#D-030） */
public enum NotificationType {
    /** 事件1：科室提交报修单 → 管理员/调度 */
    REPORT_CREATED,
    /** 事件2：派单 → 维修工 */
    DISPATCHED,
    /** 事件4：维修完成待签字 → 报修人设备 + 护士长设备 */
    PENDING_SIGN,
    /** 事件9：超时未接单 → 维修工 + 管理员 */
    TIMEOUT_UNACCEPTED
}
