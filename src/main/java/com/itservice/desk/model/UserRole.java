package com.itservice.desk.model;

public enum UserRole {
    ADMIN("管理员"),
    DISPATCHER("调度员"),
    REPAIRER("维修工"),
    REPORTER("报修人"),
    /** 财务人员：只能看/处理报销单（不接触工单与系统配置） */
    FINANCE("财务");

    private final String label;

    UserRole(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
