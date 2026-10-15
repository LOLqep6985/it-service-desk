package com.itservice.desk.model;

/**
 * 签字方式（审计留痕）：区分「手写签名」与「输入姓名生成」。
 * 说明：医院流程要求签字是报销前提，手写笔迹效力最强；
 *       TYPED 仅作为电脑工作机（无触屏）的便捷模式，界面上会明确标注。
 */
public enum SignType {
    HANDWRITTEN("手写签名"),
    TYPED("输入姓名生成");

    private final String label;

    SignType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /** 容错解析：未知/为空时回落到手写（避免脏数据把审计字段写坏） */
    public static SignType of(String raw) {
        if (raw == null || raw.isBlank()) {
            return HANDWRITTEN;
        }
        try {
            return SignType.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return HANDWRITTEN;
        }
    }
}
