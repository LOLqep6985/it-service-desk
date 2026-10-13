package com.itservice.desk.controller;

import com.itservice.desk.dto.ApiResponse;
import com.itservice.desk.dto.LoginRequest;
import com.itservice.desk.model.User;
import com.itservice.desk.repository.InMemoryStore;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final InMemoryStore store;

    public AuthController(InMemoryStore store) {
        this.store = store;
    }

    @PostMapping("/login")
    public ApiResponse login(@RequestBody LoginRequest req) {
        User user = store.usersByUsername.get(req.getUsername());
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