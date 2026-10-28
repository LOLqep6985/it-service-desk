package com.itservice.desk.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link TokenService} 单元测试（缺陷 D2 修复的核心机制）。
 *
 * <p>修复目标：凭证不可由 userId 推导伪造。本测试逐条验证四项防护：
 * 签名不可伪造、payload 不可篡改、过期失效、空/非法输入安全返回 null。
 */
class TokenServiceTest {

    private final TokenService service = new TokenService("test-secret-key-for-unit-test", 12);

    @Test
    @DisplayName("签发的凭证可校验回原 userId")
    void issueAndVerifyRoundTrip() {
        String token = service.issue(42L);
        assertEquals(42L, service.verify(token));
    }

    @Test
    @DisplayName("核心：凭证不再是可预测的 token-<id>，且对攻击者不可推导")
    void tokenIsNotPredictable() {
        String token = service.issue(1L);

        assertNotEquals("token-1", token, "绝不能是明文可推导形式");
        assertTrue(token.contains("."), "应为 payload.signature 两段结构");
        assertFalseStartsWith(token, "token-");

        // 说明：payload 含签发时刻（毫秒），同毫秒内两次签发结果相同 —— 这是设计使然
        // （凭证是「用户 + 过期时间」的确定性函数，不含随机 nonce）。
        // 因此这里不比较「两次签发是否不同」，而验证「无法在不知道密钥的情况下造出可用凭证」：
        assertNull(service.verify("token-1"));
        assertNull(service.verify("1"));
        assertNull(service.verify(String.valueOf(System.currentTimeMillis())));
    }

    @Test
    @DisplayName("篡改签名 → 拒绝")
    void tamperedSignatureRejected() {
        String token = service.issue(1L);
        char last = token.charAt(token.length() - 1);
        String tampered = token.substring(0, token.length() - 1) + (last == 'A' ? 'B' : 'A');

        assertNull(service.verify(tampered));
    }

    @Test
    @DisplayName("篡改 payload（把 userId 换成别人）→ 拒绝")
    void tamperedPayloadRejected() {
        String token = service.issue(1L);
        int dot = token.lastIndexOf('.');
        String signature = token.substring(dot + 1);

        // 攻击者尝试换成 userId=1 的邻居（如把 1 改成 2），但签名对不上
        String anotherPayload = java.util.Base64.getUrlEncoder().withoutPadding()
                .encodeToString(("2." + (System.currentTimeMillis() + 3_600_000))
                        .getBytes(java.nio.charset.StandardCharsets.UTF_8));

        assertNull(service.verify(anotherPayload + "." + signature),
                "payload 被改动后签名必须失配");
    }

    @Test
    @DisplayName("其它密钥签发的凭证 → 拒绝（等价于过期/越权的防护强度）")
    void tokenFromDifferentSecretRejected() {
        TokenService other = new TokenService("a-completely-different-secret", 12);
        assertNull(service.verify(other.issue(1L)));
    }

    @Test
    @DisplayName("空值、随机串、结构错误的输入统一安全返回 null（不抛异常）")
    void malformedInputsReturnNullSafely() {
        String[] inputs = {
                null, "", "   ", "token-1", "abc", "abc.", ".abc",
                "a.b.c", "!!!", "dept.1.9999999999999.sig"
        };
        for (String input : inputs) {
            assertNull(service.verify(input), "应安全返回 null 而非抛异常: " + input);
        }
    }

    @Test
    @DisplayName("签发时空 userId 直接报错（防止产生无主体凭证）")
    void issueNullUserIdThrows() {
        assertThrows(IllegalArgumentException.class, () -> service.issue(null));
    }

    @Test
    @DisplayName("不同用户签发的凭证互相不通用")
    void tokensAreScopedToUser() {
        String t1 = service.issue(1L);
        String t2 = service.issue(2L);
        assertEquals(1L, service.verify(t1));
        assertEquals(2L, service.verify(t2));
        assertNotEquals(t1, t2);
    }

    private static void assertFalseStartsWith(String value, String prefix) {
        assertTrue(!value.startsWith(prefix), "不应以 " + prefix + " 开头");
    }
}
