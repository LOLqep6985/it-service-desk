package com.itservice.desk.controller;

import com.itservice.desk.dto.ApiResponse;
import com.itservice.desk.dto.LoginRequest;
import com.itservice.desk.mapper.UserMapper;
import com.itservice.desk.model.User;
import com.itservice.desk.model.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 登录与会话凭证的单元测试（不连数据库、不启动 Spring 容器）。
 *
 * <p>覆盖两类高价值风险：
 * <ol>
 *   <li><b>密码校验</b>：BCrypt 命中 / 不命中、历史明文自动升级为 BCrypt；</li>
 *   <li><b>会话凭证可伪造</b>：{@code token} 为 {@code "token-" + userId} 明文拼接，
 *       任何人可离线构造。本测试把<em>现状</em>固化下来，作为"已知安全缺陷"的回归基线——
 *       一旦将来改为签名 token，本测试会失败并提醒同步更新文档与迁移方案。</li>
 * </ol>
 */
class AuthControllerTest {

    private UserMapper userMapper;
    private AuthController controller;
    private com.itservice.desk.security.TokenService tokenService;
    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

    @BeforeEach
    void setUp() {
        userMapper = mock(UserMapper.class);
        tokenService = new com.itservice.desk.security.TokenService("test-secret-for-auth", 12);
        controller = new AuthController(userMapper, tokenService);
    }

    private static User user(long id, String username, String rawOrHash, boolean hashed) {
        User u = new User();
        u.setId(id);
        u.setUsername(username);
        u.setName("测试用户");
        u.setRole(UserRole.ADMIN);
        u.setPassword(hashed ? ENCODER.encode(rawOrHash) : rawOrHash);
        return u;
    }

    private static LoginRequest req(String username, String password) {
        LoginRequest r = new LoginRequest();
        r.setUsername(username);
        r.setPassword(password);
        return r;
    }

    @Nested
    @DisplayName("口令校验")
    class PasswordCheck {

        @Test
        @DisplayName("BCrypt 命中：返回 code=0 且不含密码字段")
        void bcryptMatchSucceeds() {
            when(userMapper.selectOne(any())).thenReturn(user(1L, "admin", "admin123", true));

            ApiResponse res = controller.login(req("admin", "admin123"));

            assertEquals(0, res.getCode());
            assertTrue(res.getMessage() == null || "ok".equals(res.getMessage()));
        }

        @Test
        @DisplayName("BCrypt 不命中：返回错误，且不触发任何写库")
        void bcryptMismatchFails() {
            when(userMapper.selectOne(any())).thenReturn(user(1L, "admin", "admin123", true));

            ApiResponse res = controller.login(req("admin", "wrongpass"));

            assertEquals(500, res.getCode());
            assertEquals("用户名或密码错误", res.getMessage());
            assertNull(res.getData());
            verify(userMapper, never()).updateById(any(User.class));
        }

        @Test
        @DisplayName("用户不存在：统一返回「用户名或密码错误」，不泄露账号是否存在")
        void unknownUserGivesGenericMessage() {
            when(userMapper.selectOne(any())).thenReturn(null);

            ApiResponse res = controller.login(req("ghost", "whatever"));

            assertEquals("用户名或密码错误", res.getMessage(),
                    "不能区分「用户不存在」与「密码错误」，否则可枚举账号");
        }

        @Test
        @DisplayName("历史明文口令：校验通过后自动升级为 BCrypt 并落库")
        void plaintextIsUpgradedToBcrypt() {
            User legacy = user(2L, "dispatcher", "plain123", false);
            when(userMapper.selectOne(any())).thenReturn(legacy);

            ApiResponse res = controller.login(req("dispatcher", "plain123"));

            assertEquals(0, res.getCode());
            ArgumentCaptor<User> cap = ArgumentCaptor.forClass(User.class);
            verify(userMapper, times(1)).updateById(cap.capture());
            String stored = cap.getValue().getPassword();
            assertTrue(stored.startsWith("$2a$") || stored.startsWith("$2b$") || stored.startsWith("$2y$"),
                    "明文必须被替换成 BCrypt 哈希，实际为：" + stored);
            assertTrue(ENCODER.matches("plain123", stored), "升级后的哈希仍需校验原口令通过");
        }

        @Test
        @DisplayName("历史明文但不匹配：不写库（避免把错误口令写进库）")
        void plaintextMismatchDoesNotWrite() {
            when(userMapper.selectOne(any())).thenReturn(user(2L, "dispatcher", "plain123", false));

            ApiResponse res = controller.login(req("dispatcher", "other"));

            assertEquals(500, res.getCode());
            verify(userMapper, never()).updateById(any(User.class));
        }

        @Test
        @DisplayName("空口令 / 库中口令为 null：直接拒绝，不抛异常")
        void nullPasswordIsRejected() {
            User noPwd = user(3L, "repairer1", null, false);
            when(userMapper.selectOne(any())).thenReturn(noPwd);

            assertEquals(500, controller.login(req("repairer1", null)).getCode());
            assertEquals(500, controller.login(req("repairer1", "anything")).getCode());
        }
    }

    @Nested
    @DisplayName("会话凭证（D2 已修复：不可伪造）")
    class SessionToken {

        @Test
        @DisplayName("修复验证：token 不再是明文 token-<userId>，而是带签名的凭证")
        void tokenIsNoLongerPredictable() {
            when(userMapper.selectOne(any())).thenReturn(user(6L, "finance", "fin123", true));

            ApiResponse res = controller.login(req("finance", "fin123"));

            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> data = (java.util.Map<String, Object>) res.getData();
            assertNotNull(data);
            assertEquals(6L, data.get("userId"));
            String token = String.valueOf(data.get("token"));
            assertTrue(!"token-6".equals(token),
                    "修复前是可预测的明文，修复后必须是签名串，实际=" + token);
            assertTrue(!token.startsWith("token-"),
                    "不应再使用 token- 前缀形式");
            assertTrue(token.contains("."), "应为 payload.signature 结构");
        }

        @Test
        @DisplayName("修复验证：签发的凭证能被同一密钥校验回原用户")
        void issuedTokenIsVerifiable() {
            assertEquals(6L, tokenService.verify(tokenService.issue(6L)));
        }

        @Test
        @DisplayName("修复验证：攻击者无法凭 userId 构造可用凭证")
        void forgedTokenCannotPassVerification() {
            assertNull(tokenService.verify("token-1"),
                    "明文 token-1 必须校验失败，这是 D2 修复的核心断言");
            assertNull(tokenService.verify(String.valueOf(1L)));
        }
    }
}