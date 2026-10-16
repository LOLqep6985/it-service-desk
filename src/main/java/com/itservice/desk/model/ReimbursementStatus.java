package com.itservice.desk.model;

public enum ReimbursementStatus {
    PENDING_SIGN("待签字"),
    PENDING_REIMBURSE("待报销"),
    REIMBURSED("已报销"),
    VOIDED("已作废"),
    RETURNED("已退回");

    private final String label;

    ReimbursementStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}