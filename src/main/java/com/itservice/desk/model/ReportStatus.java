package com.itservice.desk.model;

public enum ReportStatus {
    PENDING("待受理"),
    ACCEPTED("已受理"),
    DISPATCHED("已派单"),
    CANCELLED("已取消");

    private final String label;

    ReportStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}