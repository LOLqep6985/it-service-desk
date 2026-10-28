package com.itservice.desk.controller;

import com.itservice.desk.security.AuthUserResolver;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itservice.desk.dto.ApiResponse;
import com.itservice.desk.dto.PageResult;
import com.itservice.desk.mapper.UserMapper;
import com.itservice.desk.model.User;
import com.itservice.desk.model.UserRole;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final AuthUserResolver authUserResolver;

    private final UserMapper userMapper;

    public UserController(UserMapper userMapper,
                                AuthUserResolver authUserResolver) {
        this.authUserResolver = authUserResolver;
        this.userMapper = userMapper;
    }

    @GetMapping
    public ApiResponse list(@RequestHeader(value = "X-Token", required = false) String token,
                            @RequestParam(required = false) String role,
                            @RequestParam(required = false) Integer page,
                            @RequestParam(required = false) Integer size) {
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
        // 不传 page 保持老行为（返回数组）；传了才返回分页对象
        if (page == null) {
            List<User> list = userMapper.selectList(qw);
            list.forEach(u -> u.setPassword(null));
            return ApiResponse.ok(list);
        }
        long safePage = Math.max(1, page);
        long safeSize = size == null ? 20 : Math.min(Math.max(1, size), 200);
        IPage<User> result = userMapper.selectPage(new Page<>(safePage, safeSize), qw);
        // 分页路径同样必须清除密码，否则会泄露口令哈希
        result.getRecords().forEach(u -> u.setPassword(null));
        return ApiResponse.ok(new PageResult<>(result));
    }

    private User resolveUser(String token) {
        return authUserResolver.resolve(token);
    }
}