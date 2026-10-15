package com.itservice.desk.model;

/**
 * 工单状态（文案与「设计重构」参考文档统一：待受理 → 待接单 → 维修中 → 待签字 → 已完工）。
 * 说明：PENDING（待受理）用于「已提交、尚未派单」的工单；当前实现中未派单阶段存放在 report 表，
 *       该状态预留给后续「报修单/工单单表化」时使用，因此当前不会出现。
 *       后续版本再补：已挂起（可回到挂起前状态）、已驳回（退回维修中）、已取消。
 */
public enum WorkOrderStatus {
    PENDING("待受理"),
    ASSIGNED("待接单"),
    IN_PROGRESS("维修中"),
    PENDING_ACCEPT("待签字"),
    COMPLETED("已完工");

    private final String label;

    WorkOrderStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
