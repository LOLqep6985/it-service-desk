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

    /**
     * 科室端自助登记设备（免登录）：deviceId → 科室 + 是否护士长设备。
     *
     * 为什么需要：「待签字」消息要只发给报修人设备与护士长设备，而科室端的科室选择
     * 原本只存在本地 localStorage，服务端不知道某台设备属于谁。这里是补上的最小登记入口。
     *
     * 安全说明：与免登录报修同等信任级别（内网可用，不做口令校验），属已知弱凭证限制。
     */
    @PostMapping("/self")
    public ApiResponse selfBind(@RequestBody Map<String, Object> req) {
        String deviceId = req.get("deviceId") == null ? null : String.valueOf(req.get("deviceId")).trim();
        if (deviceId == null || deviceId.length() < 4 || deviceId.length() > 64) {
            return ApiResponse.error("设备标识无效（长度需 4-64）");
        }
        Long deptId;
        try {
            deptId = Long.valueOf(String.valueOf(req.get("departmentId")).trim());
        } catch (Exception e) {
            return ApiResponse.error("请选择有效的科室");
        }
        Department dept = departmentMapper.selectById(deptId);
        if (dept == null) {
            return ApiResponse.error("科室不存在");
        }
        boolean isHead = "true".equalsIgnoreCase(String.valueOf(req.get("isHead")));

        // 同一设备旧绑定置为失效（重绑/换科室）
        List<DepartmentBinding> old = bindingMapper.selectList(new LambdaQueryWrapper<DepartmentBinding>()
                .eq(DepartmentBinding::getDeviceCode, deviceId));
        for (DepartmentBinding o : old) {
            o.setActive(false);
            bindingMapper.updateById(o);
        }
        DepartmentBinding b = new DepartmentBinding();
        b.setDepartmentId(dept.getId());
        b.setDeviceCode(deviceId);
        b.setBindAt(LocalDateTime.now());
        b.setActive(true);
        b.setIsHead(isHead);
        bindingMapper.insert(b);

        Map<String, Object> data = new HashMap<>();
        data.put("bindingId", b.getId());
        data.put("departmentId", dept.getId());
        data.put("departmentName", dept.getName());
        data.put("isHead", isHead);
        return ApiResponse.ok(data);
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
        data.put("departmentHead", dept.getDepartmentHead());
        data.put("isHead", Boolean.TRUE.equals(b.getIsHead()));
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