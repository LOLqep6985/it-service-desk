package com.itservice.desk.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itservice.desk.dto.ApiResponse;
import com.itservice.desk.dto.LoginRequest;
import com.itservice.desk.mapper.UserMapper;
import com.itservice.desk.model.User;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserMapper userMapper;

    public AuthController(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @PostMapping("/login")
    public ApiResponse login(@RequestBody LoginRequest req) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, req.getUsername()));
        if (user == null || !user.getPassword().equals(req.getPassword())) {
            return ApiResponse.error("用户名或密码错误");
        }
        Map<String, Object> data = new HashMap<>();
        data.put("userId", user.getId());
        data.put("name", user.getName());
        data.put("role", user.getRole().getLabel());
        data.put("token", "token-" + user.getId());
        return ApiResponse.ok(data);
    }
}