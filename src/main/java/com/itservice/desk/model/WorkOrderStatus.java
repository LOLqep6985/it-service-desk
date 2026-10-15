package com.itservice.desk.model;

/**
 * 工单主状态（严格对齐《工单全流程规则.md》的 6 种主状态）。
 *
 * 流转：
 *   派单 → ASSIGNED(待接单)
 *     → 维修工接单 → IN_PROGRESS(维修中)
 *     → 维修提交关闭 → PENDING_PRICING(待核价)
 *     → 管理员核价完成 → PENDING_SIGN(待签字)
 *     → 护士长签字验收 → PENDING_REIMBURSE(待报销)【移出工单管理，进入工单历史】
 *     → 月度报销 → REIMBURSED(已报销)
 *
 * 页面可见性：
 *   工单管理（进行中）= ASSIGNED + IN_PROGRESS + PENDING_PRICING + PENDING_SIGN
 *   工单历史 = PENDING_REIMBURSE + REIMBURSED
 */
public enum WorkOrderStatus {
    ASSIGNED("待接单"),
    IN_PROGRESS("维修中"),
    PENDING_PRICING("待核价"),
    PENDING_SIGN("待签字"),
    PENDING_REIMBURSE("待报销"),
    REIMBURSED("已报销");

    private final String label;

    WorkOrderStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
