package com.itservice.desk.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link DeptTokenService} 单元测试（缺陷 D3 修复的核心机制）。
 *
 * <p>修复目标：科室身份不再来自"调用方报的 departmentId 数字"，
 * 而是来自服务端签名、可校验、可过期的科室凭证。
 */
class DeptTokenServiceTest {

    private final DeptTokenService service = new DeptTokenService("test-dept-secret-for-unit-test", 720);

    @Test
    @DisplayName("签发后可校验回原科室 ID")
    void issueAndVerifyRoundTrip() {
        assertEquals(6L, service.verify(service.issue(6L)));
    }

    @Test
    @DisplayName("核心：裸数字（含原始 departmentId）不再是有效凭证")
    void bareNumberIsNotAValidToken() {
        assertNull(service.verify("6"), "修复前 'departmentId=6' 直接生效，现在必须无效");
        assertNull(service.verify("1"));
        assertNull(service.verify("73"));
    }

    @Test
    @DisplayName("篡改签名 → 拒绝")
    void tamperedSignatureRejected() {
        String token = service.issue(6L);
        char last = token.charAt(token.length() - 1);
        assertNull(service.verify(token.substring(0, token.length() - 1) + (last == 'A' ? 'B' : 'A')));
    }

    @Test
    @DisplayName("伪造成别的科室 → 签名失配被拒")
    void cannotRewriteDepartmentId() {
        String forSix = service.issue(6L);
        String signature = forSix.substring(forSix.lastIndexOf('.') + 1);
        String forgedPayload = java.util.Base64.getUrlEncoder().withoutPadding()
                .encodeToString(("dept.7." + (System.currentTimeMillis() + 86_400_000))
                        .getBytes(java.nio.charset.StandardCharsets.UTF_8));

        assertNull(service.verify(forgedPayload + "." + signature),
                "把 6 改成 7 必须导致签名失配 —— 这正是修复的核心价值");
    }

    @Test
    @DisplayName("其它密钥签发 → 拒绝")
    void tokenFromDifferentSecretRejected() {
        DeptTokenService other = new DeptTokenService("a-different-dept-secret", 720);
        assertNull(service.verify(other.issue(6L)));
    }

    @Test
    @DisplayName("空值、随机串、结构错误统一安全返回 null")
    void malformedInputsReturnNullSafely() {
        String[] inputs = {null, "", "  ", "dept", "dept.", ".dept", "dept.6",
                "dept.6.", "dept.6.9999999999999", "a.b.c.d", "!!!"};
        for (String input : inputs) {
            assertNull(service.verify(input), "应安全返回 null: " + input);
        }
    }

    @Test
    @DisplayName("凭证带 dept 前缀，与用户凭证结构区分")
    void tokenIsPrefixedAndDistinct() {
        String token = service.issue(6L);
        assertTrue(token.contains("."));
        assertNotEquals(service.issue(6L), service.issue(7L));
    }

    @Test
    @DisplayName("签发时空科室 ID 直接报错")
    void issueNullDepartmentThrows() {
        assertThrows(IllegalArgumentException.class, () -> service.issue(null));
    }
}
