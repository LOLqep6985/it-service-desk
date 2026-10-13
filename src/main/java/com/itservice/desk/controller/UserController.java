package com.itservice.desk.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itservice.desk.dto.ApiResponse;
import com.itservice.desk.mapper.UserMapper;
import com.itservice.desk.model.User;
import com.itservice.desk.model.UserRole;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserMapper userMapper;

    public UserController(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @GetMapping
    public ApiResponse list(@RequestHeader(value = "X-Token", required = false) String token,
                            @RequestParam(required = false) String role) {
        User viewer = resolveUser(token);
        if (viewer == null) {
            return ApiResponse.error(401, "未登录或登录已过期");
        }
        if (viewer.getRole() != UserRole.ADMIN && viewer.getRole() != UserRole.DISPATCHER) {
            return ApiResponse.error(403, "无权限：只有管理员或调度员可以查看用户列表");
        }
        LambdaQueryWrapper<User> qw = new LambdaQueryWrapper<User>().orderByAsc(User::getId);
        if (role != null && !role.isBlank()) {
            try {
                qw.eq(User::getRole, UserRole.valueOf(role));
            } catch (IllegalArgumentException e) {
                return ApiResponse.error("无效的角色参数：" + role);
            }
        }
        List<User> list = userMapper.selectList(qw);
        // 不返回密码
        list.forEach(u -> u.setPassword(null));
        return ApiResponse.ok(list);
    }

    private User resolveUser(String token) {
        if (token == null || !token.startsWith("token-")) {
            return null;
        }
        try {
            Long userId = Long.valueOf(token.substring("token-".length()));
            return userMapper.selectById(userId);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}