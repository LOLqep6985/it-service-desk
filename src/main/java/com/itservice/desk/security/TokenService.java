package com.itservice.desk.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

/**
 * 会话凭证签发与校验（缺陷 D2 修复）。
 *
 * <h2>修复前的问题</h2>
 * token 形如 {@code "token-" + userId}，完全由 userId 推导，不含随机数、不签名、不过期。
 * 任何人只需构造 {@code X-Token: token-1} 即可冒充管理员（已实测 HTTP 200 返回真实数据）。
 *
 * <h2>修复方案</h2>
 * 采用 <b>HMAC-SHA256 签名 + 时间戳</b> 的无状态凭证：
 * <pre>
 *   token = base64url(payload) + "." + base64url(hmacSha256(payload, secret))
 *   payload = "userId.expireAtMillis"
 * </pre>
 * 校验三件事，任一不满足即拒绝：
 * <ol>
 *   <li>格式合法（恰好两段）；</li>
 *   <li><b>签名匹配</b>（攻击者无密钥无法伪造）；</li>
 *   <li><b>未过期</b>（默认 12 小时，可通过配置调整）。</li>
 * </ol>
 *
 * <h2>为什么不用 JWT</h2>
 * 本项目仅需"证明身份 + 防篡改 + 过期"，HMAC 签名已足够；
 * 引入 JWT 库会新增依赖，且离线仓库尚未缓存，违背"技术选型服务问题"的项目原则。
 * 签名算法与校验逻辑同构，日后替换为 JWT 只需改本类。
 *
 * <h2>密钥来源</h2>
 * 通过 {@code auth.token-secret} 配置，默认值仅供本地开发。
 * <b>生产部署必须通过环境变量 {@code AUTH_TOKEN_SECRET} 覆盖</b>，
 * 否则重启/多实例场景下签名密钥不一致。配置缺失时会在启动日志中告警。
 */
@Component
public class TokenService {

    private static final Logger log = LoggerFactory.getLogger(TokenService.class);

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String SEPARATOR = ".";

    private final byte[] secret;
    private final long ttlMillis;

    public TokenService(@Value("${auth.token-secret:}") String secret,
                        @Value("${auth.token-ttl-hours:12}") long ttlHours) {
        if (secret == null || secret.isBlank()) {
            log.warn("未配置 auth.token-secret / AUTH_TOKEN_SECRET，正在使用内置开发密钥。"
                    + "生产环境务必通过环境变量覆盖，否则凭证可被伪造！");
            secret = "itsd-dev-only-insecure-secret-please-override";
        }
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.ttlMillis = Math.max(1, ttlHours) * 60L * 60L * 1000L;
    }

    /** 为指定用户签发凭证 */
    public String issue(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId 不能为空");
        }
        long expireAt = System.currentTimeMillis() + ttlMillis;
        String payload = userId + SEPARATOR + expireAt;
        String encodedPayload = base64Url(payload.getBytes(StandardCharsets.UTF_8));
        return encodedPayload + SEPARATOR + sign(encodedPayload);
    }

    /**
     * 校验凭证，合法则返回 userId，否则返回 {@code null}。
     * <b>任何异常都视为不合法</b>（不区分"格式错"与"签名错"，避免给攻击者反馈）。
     */
    public Long verify(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        int idx = token.lastIndexOf(SEPARATOR);
        if (idx <= 0 || idx == token.length() - 1) {
            return null;
        }
        String encodedPayload = token.substring(0, idx);
        String providedSignature = token.substring(idx + 1);

        String expectedSignature = sign(encodedPayload);
        // 常量时间比较，防时序侧信道
        if (!MessageDigest.isEqual(
                expectedSignature.getBytes(StandardCharsets.UTF_8),
                providedSignature.getBytes(StandardCharsets.UTF_8))) {
            return null;
        }

        try {
            String payload = new String(Base64.getUrlDecoder().decode(encodedPayload), StandardCharsets.UTF_8);
            int sep = payload.lastIndexOf(SEPARATOR);
            if (sep <= 0) {
                return null;
            }
            Long userId = Long.valueOf(payload.substring(0, sep));
            long expireAt = Long.parseLong(payload.substring(sep + 1));
            if (System.currentTimeMillis() > expireAt) {
                return null;   // 已过期
            }
            return userId;
        } catch (RuntimeException e) {
            return null;
        }
    }

    private String sign(String encodedPayload) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret, HMAC_ALGORITHM));
            return base64Url(mac.doFinal(encodedPayload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            // 签名失败属于致命配置问题，直接抛出而不是静默返回 null
            throw new IllegalStateException("凭证签名失败", e);
        }
    }

    private static String base64Url(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
