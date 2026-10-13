package com.itservice.desk.model;

public enum SignStatus {
    UNSIGNED("待签字"),
    SIGNED("已签字");

    private final String label;

    SignStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}