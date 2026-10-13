package com.itservice.desk.model;

public enum Campus {
    LAOYUAN("老院"),
    XIYUAN("西院"),
    DONGYUAN("东院");

    private final String label;

    Campus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}