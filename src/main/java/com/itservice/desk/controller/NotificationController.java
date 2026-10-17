package com.itservice.desk.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itservice.desk.dto.ApiResponse;
import com.itservice.desk.mapper.DepartmentBindingMapper;
import com.itservice.desk.mapper.UserMapper;
import com.itservice.desk.model.DepartmentBinding;
import com.itservice.desk.model.User;
import com.itservice.desk.service.NotificationService;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 站内消息接口（A 档，轮询）。
 *
 * 身份识别：
 *  - 有 X-Token → 登录用户（管理员/调度/维修工）
 *  - 无 token 但带 deviceCode → 科室端设备（免登录，凭 department_binding.device_code）
 */
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final UserMapper userMapper;
    private final DepartmentBindingMapper bindingMapper;

    public NotificationController(NotificationService notificationService,
                                  UserMapper userMapper,
                                  DepartmentBindingMapper bindingMapper) {
        this.notificationService = notificationService;
        this.userMapper = userMapper;
        this.bindingMapper = bindingMapper;
    }

    @GetMapping
    public ApiResponse list(@RequestHeader(value = "X-Token", required = false) String token,
                            @RequestParam(value = "deviceCode", required = false) String deviceCode,
                            @RequestParam(value = "since", defaultValue = "0") long since,
                            @RequestParam(value = "limit", defaultValue = "20") int limit) {
        User user = resolveUser(token);
        if (user != null) {
            return ApiResponse.ok(notificationService.listFor(NotificationService.KIND_USER, user.getId(), since, limit));
        }
        DepartmentBinding binding = resolveBinding(deviceCode);
        if (binding != null) {
            return ApiResponse.ok(notificationService.listFor(NotificationService.KIND_DEVICE, binding.getId(), since, limit));
        }
        return ApiResponse.error(401, "未登录或设备未绑定科室");
    }

    @PostMapping("/read")
    public ApiResponse read(@RequestHeader(value = "X-Token", required = false) String token,
                            @RequestParam(value = "deviceCode", required = false) String deviceCode,
                            @RequestBody(required = false) Map<String, Object> body) {
        String kind;
        Long targetId;
        User user = resolveUser(token);
        if (user != null) {
            kind = NotificationService.KIND_USER;
            targetId = user.getId();
        } else {
            DepartmentBinding binding = resolveBinding(deviceCode);
            if (binding == null) {
                return ApiResponse.error(401, "未登录或设备未绑定科室");
            }
            kind = NotificationService.KIND_DEVICE;
            targetId = binding.getId();
        }

        boolean all = body != null && Boolean.TRUE.equals(body.get("all"));
        if (all) {
            return ApiResponse.ok(notificationService.markAllRead(kind, targetId));
        }
        List<Long> ids = new ArrayList<>();
        if (body != null && body.get("ids") instanceof List<?> raw) {
            for (Object o : raw) {
                if (o instanceof Number num) {
                    ids.add(num.longValue());
                }
            }
        }
        return ApiResponse.ok(notificationService.markRead(kind, targetId, ids));
    }

    private User resolveUser(String token) {
        if (token == null || !token.startsWith("token-")) {
            return null;
        }
        try {
            return userMapper.selectById(Long.valueOf(token.substring("token-".length())));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private DepartmentBinding resolveBinding(String deviceCode) {
        if (deviceCode == null || deviceCode.isBlank()) {
            return null;
        }
        return bindingMapper.selectOne(new LambdaQueryWrapper<DepartmentBinding>()
                .eq(DepartmentBinding::getDeviceCode, deviceCode)
                .eq(DepartmentBinding::getActive, true)
                .last("LIMIT 1"));
    }
}
