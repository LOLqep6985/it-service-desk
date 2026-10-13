package com.itservice.desk.model;

public enum UserRole {
    ADMIN("管理员"),
    DISPATCHER("调度员"),
    REPAIRER("维修工"),
    REPORTER("报修人");

    private final String label;

    UserRole(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}