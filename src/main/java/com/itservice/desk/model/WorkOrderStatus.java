package com.itservice.desk.model;

public enum WorkOrderStatus {
    PENDING("待派单"),
    ASSIGNED("已派单"),
    IN_PROGRESS("处理中"),
    PENDING_ACCEPT("待科室验收"),
    COMPLETED("已完成");

    private final String label;

    WorkOrderStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}