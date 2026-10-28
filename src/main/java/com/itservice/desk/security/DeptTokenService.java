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
 * 科室访问凭证（缺陷 D3 修复）。
 *
 * <h2>修复前的漏洞</h2>
 * {@code resolveDepartmentId(departmentId, deviceCode)} 的逻辑是：
 * <pre>
 *   if (departmentId != null) return departmentId;   // 直接信任调用方报的科室 ID
 *   ...deviceCode 路径才会校验绑定关系
 * </pre>
 * 于是任何人只要遍历 {@code departmentId=1..N} 就能读取全院报修单与工单（已实测 HTTP 200）。
 *
 * <h2>为什么不能简单"禁用裸 departmentId"</h2>
 * 科室端（{@code report.html}）是<b>免登录</b>页面，且大量接口用 {@code ?departmentId=N} 调用。
 * 直接禁掉会让整个科室端瘫痪。
 *
 * <h2>修复方案：把"报一个数字"升级为"出示一张签过名的科室凭证"</h2>
 * <ol>
 *   <li>科室端在<b>完成设备自助登记</b>（{@code POST /api/bindings/self}）后，
 *       由服务端签发一张 {@code deptToken}，存放于 localStorage；</li>
 *   <li>后续所有需要科室身份的接口改为携带 {@code deptToken}；</li>
 *   <li>服务端<b>用签名反解出 departmentId</b>，不再采信调用方传入的数字。</li>
 * </ol>
 *
 * <h2>安全边界（如实声明）</h2>
 * 本方案把攻击门槛从"猜一个数字"提高到"必须先登记设备再拿凭证"，
 * 但 {@code /api/bindings/self} 本身免登录，<b>仍可通过登记任意设备获取任意科室凭证</b>。
 * 这是项目既有的弱身份模型（内网、免登录）决定的，
 * <b>不是本次修复能彻底解决的问题</b>；彻底解决需为科室端引入口令或由管理员预分配设备码。
 * 本次修复消除的是"零成本遍历"这一最严重形态。
 */
@Component
public class DeptTokenService {

    private static final Logger log = LoggerFactory.getLogger(DeptTokenService.class);

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String SEPARATOR = ".";
    /** 前缀用于区分科室凭证与登录用户凭证，防止两类凭证互相冒用 */
    private static final String PREFIX = "dept";

    private final byte[] secret;
    private final long ttlMillis;

    public DeptTokenService(@Value("${auth.dept-token-secret:}") String secret,
                            @Value("${auth.dept-token-ttl-hours:720}") long ttlHours) {
        if (secret == null || secret.isBlank()) {
            log.warn("未配置 auth.dept-token-secret / AUTH_DEPT_TOKEN_SECRET，使用内置开发密钥。"
                    + "生产环境务必覆盖！");
            secret = "itsd-dev-only-insecure-dept-secret-please-override";
        }
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.ttlMillis = Math.max(1, ttlHours) * 60L * 60L * 1000L;
    }

    /** 绑定成功后签发科室凭证 */
    public String issue(Long departmentId) {
        if (departmentId == null) {
            throw new IllegalArgumentException("departmentId 不能为空");
        }
        long expireAt = System.currentTimeMillis() + ttlMillis;
        String payload = PREFIX + SEPARATOR + departmentId + SEPARATOR + expireAt;
        String encoded = base64Url(payload.getBytes(StandardCharsets.UTF_8));
        return encoded + SEPARATOR + sign(encoded);
    }

    /** 校验科室凭证，合法返回 departmentId，否则返回 null */
    public Long verify(String deptToken) {
        if (deptToken == null || deptToken.isBlank()) {
            return null;
        }
        int idx = deptToken.lastIndexOf(SEPARATOR);
        if (idx <= 0 || idx == deptToken.length() - 1) {
            return null;
        }
        String encoded = deptToken.substring(0, idx);
        String provided = deptToken.substring(idx + 1);
        if (!MessageDigest.isEqual(
                sign(encoded).getBytes(StandardCharsets.UTF_8),
                provided.getBytes(StandardCharsets.UTF_8))) {
            return null;
        }
        try {
            String payload = new String(Base64.getUrlDecoder().decode(encoded), StandardCharsets.UTF_8);
            String[] parts = payload.split("\\" + SEPARATOR);
            if (parts.length != 3 || !PREFIX.equals(parts[0])) {
                return null;
            }
            Long departmentId = Long.valueOf(parts[1]);
            long expireAt = Long.parseLong(parts[2]);
            if (System.currentTimeMillis() > expireAt) {
                return null;
            }
            return departmentId;
        } catch (RuntimeException e) {
            return null;
        }
    }

    private String sign(String encoded) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret, HMAC_ALGORITHM));
            return base64Url(mac.doFinal(encoded.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("科室凭证签名失败", e);
        }
    }

    private static String base64Url(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
