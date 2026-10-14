package com.itservice.desk.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itservice.desk.dto.ApiResponse;
import com.itservice.desk.dto.LoginRequest;
import com.itservice.desk.mapper.UserMapper;
import com.itservice.desk.model.User;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 登录：密码使用 BCrypt 校验。
 * 兼容历史明文数据：若库中存的不是 BCrypt 哈希（$2a$/$2b$/$2y$ 开头），
 * 按明文比对成功后**自动升级为 BCrypt 哈希**，用户无感知。
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

    private final UserMapper userMapper;

    public AuthController(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @PostMapping("/login")
    public ApiResponse login(@RequestBody LoginRequest req) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, req.getUsername()));
        if (user == null || !passwordMatches(req.getPassword(), user)) {
            return ApiResponse.error("用户名或密码错误");
        }
        Map<String, Object> data = new HashMap<>();
        data.put("userId", user.getId());
        data.put("name", user.getName());
        data.put("role", user.getRole().getLabel());
        data.put("roleCode", user.getRole().name());
        data.put("token", "token-" + user.getId());
        return ApiResponse.ok(data);
    }

    /** 校验密码；历史明文命中后自动升级为 BCrypt */
    private boolean passwordMatches(String raw, User user) {
        if (raw == null || user.getPassword() == null) {
            return false;
        }
        String stored = user.getPassword();
        if (isBcrypt(stored)) {
            return ENCODER.matches(raw, stored);
        }
        if (!stored.equals(raw)) {
            return false;
        }
        user.setPassword(ENCODER.encode(raw));
        userMapper.updateById(user);
        return true;
    }

    private boolean isBcrypt(String stored) {
        return stored.startsWith("$2a$") || stored.startsWith("$2b$") || stored.startsWith("$2y$");
    }
}
