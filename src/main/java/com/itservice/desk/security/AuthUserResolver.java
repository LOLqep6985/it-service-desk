package com.itservice.desk.security;

import com.itservice.desk.mapper.UserMapper;
import com.itservice.desk.model.User;
import org.springframework.stereotype.Component;

/**
 * 凭证解析的唯一入口（缺陷 D2 修复的配套重构）。
 *
 * <h2>为什么抽出来</h2>
 * 修复前，9 个 Controller 各自持有一份<b>逐字重复</b>的 {@code resolveUser(token)}：
 * <pre>
 *   if (token == null || !token.startsWith("token-")) return null;
 *   Long userId = Long.valueOf(token.substring("token-".length()));
 *   return userMapper.selectById(userId);
 * </pre>
 * 这带来两个问题：
 * <ol>
 *   <li><b>安全逻辑分散</b>：改凭证机制要改 9 处，漏一处就是一个后门；</li>
 *   <li><b>无法统一演进</b>：想加过期、加签名、加黑名单，必须逐个文件改。</li>
 * </ol>
 * 本类把"token 字符串 → User 实体"收敛为唯一实现，Controller 只依赖此组件。
 *
 * <p>返回值语义：{@code null} 表示"未认证"（调用方应返回 401）。
 */
@Component
public class AuthUserResolver {

    private final TokenService tokenService;
    private final UserMapper userMapper;

    public AuthUserResolver(TokenService tokenService, UserMapper userMapper) {
        this.tokenService = tokenService;
        this.userMapper = userMapper;
    }

    /**
     * 解析凭证。签名不合法、已过期、用户不存在，一律返回 {@code null}。
     */
    public User resolve(String token) {
        Long userId = tokenService.verify(token);
        if (userId == null) {
            return null;
        }
        return userMapper.selectById(userId);
    }
}
