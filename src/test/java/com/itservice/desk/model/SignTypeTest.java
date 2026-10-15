package com.itservice.desk.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 签字方式枚举的容错解析测试（项目首个后端单元测试）。
 * 关注点：客户端可能传空/大小写不一/脏值，审计字段不允许写坏。
 */
class SignTypeTest {

    @Test
    @DisplayName("签名为空时默认按手写处理")
    void nullDefaultsToHandwritten() {
        assertEquals(SignType.HANDWRITTEN, SignType.of(null));
    }

    @Test
    @DisplayName("空白字符串默认按手写处理")
    void blankDefaultsToHandwritten() {
        assertEquals(SignType.HANDWRITTEN, SignType.of("   "));
    }

    @Test
    @DisplayName("小写与带空格也能解析")
    void parsesLooseInput() {
        assertEquals(SignType.TYPED, SignType.of("typed"));
        assertEquals(SignType.TYPED, SignType.of("  TyPeD  "));
    }

    @Test
    @DisplayName("标准值原样解析")
    void parsesExactValues() {
        assertEquals(SignType.HANDWRITTEN, SignType.of("HANDWRITTEN"));
        assertEquals(SignType.TYPED, SignType.of("TYPED"));
    }

    @Test
    @DisplayName("无法识别的脏值回落到手写，避免污染审计字段")
    void unknownFallsBackToHandwritten() {
        assertEquals(SignType.HANDWRITTEN, SignType.of("SCRIBBLE"));
        assertEquals(SignType.HANDWRITTEN, SignType.of("123"));
    }

    @Test
    @DisplayName("中文标签与界面展示一致")
    void labelsAreHumanReadable() {
        assertEquals("手写签名", SignType.HANDWRITTEN.getLabel());
        assertEquals("输入姓名生成", SignType.TYPED.getLabel());
    }
}
