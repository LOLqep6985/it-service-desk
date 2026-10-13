package com.itservice.desk.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itservice.desk.dto.ApiResponse;
import com.itservice.desk.dto.BindingCreateRequest;
import com.itservice.desk.mapper.DepartmentBindingMapper;
import com.itservice.desk.mapper.DepartmentMapper;
import com.itservice.desk.mapper.UserMapper;
import com.itservice.desk.model.Department;
import com.itservice.desk.model.DepartmentBinding;
import com.itservice.desk.model.User;
import com.itservice.desk.model.UserRole;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bindings")
public class DepartmentBindingController {

    private final DepartmentBindingMapper bindingMapper;
    private final DepartmentMapper departmentMapper;
    private final UserMapper userMapper;

    public DepartmentBindingController(DepartmentBindingMapper bindingMapper,
                                       DepartmentMapper departmentMapper,
                                       UserMapper userMapper) {
        this.bindingMapper = bindingMapper;
        this.departmentMapper = departmentMapper;
        this.userMapper = userMapper;
    }

    @PostMapping
    public ApiResponse bind(@RequestBody BindingCreateRequest req,
                            @RequestHeader(value = "X-Token", required = false) String token) {
        User user = resolveUser(token);
        if (user == null) {
            return ApiResponse.error(401, "未登录或登录已过期");
        }
        if (user.getRole() != UserRole.ADMIN && user.getRole() != UserRole.DISPATCHER) {
            return ApiResponse.error(403, "无权限：只有管理员或调度员可以绑定设备");
        }
        Department dept = departmentMapper.selectById(req.getDepartmentId());
        if (dept == null) {
            return ApiResponse.error("科室不存在");
        }
        // 同一设备旧绑定置为失效
        List<DepartmentBinding> old = bindingMapper.selectList(new LambdaQueryWrapper<DepartmentBinding>()
                .eq(DepartmentBinding::getDeviceCode, req.getDeviceCode()));
        for (DepartmentBinding b : old) {
            b.setActive(false);
            bindingMapper.updateById(b);
        }
        DepartmentBinding b = new DepartmentBinding();
        b.setDepartmentId(dept.getId());
        b.setDeviceCode(req.getDeviceCode());
        b.setBindAt(LocalDateTime.now());
        b.setActive(true);
        bindingMapper.insert(b);
        return ApiResponse.ok(b);
    }

    @GetMapping("/info")
    public ApiResponse info(@RequestParam String deviceCode) {
        DepartmentBinding b = bindingMapper.selectOne(new LambdaQueryWrapper<DepartmentBinding>()
                .eq(DepartmentBinding::getDeviceCode, deviceCode)
                .eq(DepartmentBinding::getActive, true));
        if (b == null) {
            return ApiResponse.error("设备码未绑定科室");
        }
        Department dept = departmentMapper.selectById(b.getDepartmentId());
        if (dept == null) {
            return ApiResponse.error("绑定的科室不存在");
        }
        Map<String, Object> data = new HashMap<>();
        data.put("bindingId", b.getId());
        data.put("departmentId", dept.getId());
        data.put("departmentName", dept.getName());
        data.put("campus", dept.getCampus());
        return ApiResponse.ok(data);
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