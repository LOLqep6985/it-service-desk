package com.itservice.desk.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itservice.desk.dto.*;
import com.itservice.desk.mapper.ConsumableMapper;
import com.itservice.desk.mapper.DepartmentBindingMapper;
import com.itservice.desk.mapper.UserMapper;
import com.itservice.desk.model.*;
import com.itservice.desk.service.WorkOrderService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/work-orders")
public class WorkOrderController {

    private final WorkOrderService workOrderService;
    private final UserMapper userMapper;
    private final DepartmentBindingMapper bindingMapper;
    private final ConsumableMapper consumableMapper;

    public WorkOrderController(WorkOrderService workOrderService,
                                UserMapper userMapper,
                                DepartmentBindingMapper bindingMapper,
                                ConsumableMapper consumableMapper) {
        this.workOrderService = workOrderService;
        this.userMapper = userMapper;
        this.bindingMapper = bindingMapper;
        this.consumableMapper = consumableMapper;
    }

    @PostMapping
    public ApiResponse create(@RequestBody WorkOrderCreateRequest req,
                            @RequestHeader(value = "X-Token", required = false) String token) {
        User user = resolveUserByToken(token);
        if (user == null) {
            return ApiResponse.error(401, "未登录或登录已过期");
        }
        if (user.getRole() != UserRole.REPORTER) {
            return ApiResponse.error(403, "无权限：只有报修人可以发起报修");
        }
        try {
            WorkOrder wo = workOrderService.createWorkOrder(req, user.getId());
            return ApiResponse.ok(wo);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @GetMapping
    public ApiResponse list(@RequestHeader(value = "X-Token", required = false) String token,
                            @RequestParam(required = false) String status) {
        User user = resolveUserByToken(token);
        if (user == null) {
            return ApiResponse.error(401, "未登录或登录已过期");
        }
        if (user.getRole() != UserRole.ADMIN && user.getRole() != UserRole.DISPATCHER) {
            return ApiResponse.error(403, "无权限：只有管理员或调度员可以查看工单列表");
        }
        return ApiResponse.ok(workOrderService.listAll(status));
    }

    @GetMapping("/today")
    public ApiResponse today(@RequestHeader(value = "X-Token", required = false) String token) {
        User user = resolveUserByToken(token);
        if (user == null) {
            return ApiResponse.error(401, "未登录或登录已过期");
        }
        List<WorkOrder> list = workOrderService.listTodayTasks(user.getId());
        return ApiResponse.ok(list);
    }

    @GetMapping("/pending-accept")
    public ApiResponse pendingAccept(@RequestParam String deviceCode) {
        DepartmentBinding b = findActiveBinding(deviceCode);
        if (b == null) {
            return ApiResponse.error(401, "设备码未绑定科室");
        }
        return ApiResponse.ok(workOrderService.listPendingAcceptByDepartment(b.getDepartmentId()));
    }

    @GetMapping("/{id}")
    public ApiResponse detail(@PathVariable Long id,
                              @RequestHeader(value = "X-Token", required = false) String token,
                              @RequestParam(required = false) String deviceCode) {
        WorkOrder wo = workOrderService.getById(id);
        // 科室端免登录：设备码必须匹配工单科室
        if (deviceCode != null && !deviceCode.isBlank()) {
            DepartmentBinding b = findActiveBinding(deviceCode);
            if (b == null || !b.getDepartmentId().equals(wo.getDepartmentId())) {
                return ApiResponse.error(403, "无权查看该工单");
            }
            return ApiResponse.ok(wo);
        }
        // 登录用户：任意已登录角色可看（报修人/维修工/管理员各自场景）
        User user = resolveUserByToken(token);
        if (user == null) {
            return ApiResponse.error(401, "未登录或登录已过期");
        }
        return ApiResponse.ok(wo);
    }

    @GetMapping("/{id}/consumables")
    public ApiResponse consumables(@PathVariable Long id,
                                   @RequestHeader(value = "X-Token", required = false) String token,
                                   @RequestParam(required = false) String deviceCode) {
        WorkOrder wo = workOrderService.getById(id);
        if (deviceCode != null && !deviceCode.isBlank()) {
            DepartmentBinding b = findActiveBinding(deviceCode);
            if (b == null || !b.getDepartmentId().equals(wo.getDepartmentId())) {
                return ApiResponse.error(403, "无权查看该工单的耗材记录");
            }
            return ApiResponse.ok(workOrderService.listConsumables(id));
        }
        User user = resolveUserByToken(token);
        if (user == null) {
            return ApiResponse.error(401, "未登录或登录已过期");
        }
        boolean allowed = user.getRole() == UserRole.ADMIN
                || user.getRole() == UserRole.DISPATCHER
                || (user.getRole() == UserRole.REPAIRER && user.getId().equals(wo.getAssigneeId()));
        if (!allowed) {
            return ApiResponse.error(403, "无权查看该工单的耗材记录");
        }
        return ApiResponse.ok(workOrderService.listConsumables(id));
    }

    @PostMapping("/{id}/accept")
    public ApiResponse accept(@PathVariable Long id,
                              @RequestHeader(value = "X-Token", required = false) String token) {
        User user = resolveUserByToken(token);
        if (user == null) {
            return ApiResponse.error(401, "未登录或登录已过期");
        }
        if (user.getRole() != UserRole.REPAIRER) {
            return ApiResponse.error(403, "无权限：只有维修人员可以接单");
        }
        try {
            WorkOrder wo = workOrderService.getById(id);
            if (!user.getId().equals(wo.getAssigneeId())) {
                return ApiResponse.error(403, "该工单不是派给你的");
            }
            return ApiResponse.ok(workOrderService.accept(id));
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @PostMapping("/{id}/repair")
    public ApiResponse repair(@PathVariable Long id,
                              @RequestBody RepairRequest req,
                              @RequestHeader(value = "X-Token", required = false) String token) {
        User user = resolveUserByToken(token);
        if (user == null) {
            return ApiResponse.error(401, "未登录或登录已过期");
        }
        if (user.getRole() != UserRole.REPAIRER) {
            return ApiResponse.error(403, "无权限：只有维修人员可以提交维修记录");
        }
        try {
            WorkOrder wo0 = workOrderService.getById(id);
            if (!user.getId().equals(wo0.getAssigneeId())) {
                return ApiResponse.error(403, "该工单不是派给你的");
            }
            WorkOrder wo = workOrderService.submitRepair(id, req.getRecord(), req.getConsumables());
            return ApiResponse.ok(wo);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @PostMapping("/{id}/sign")
    public ApiResponse sign(@PathVariable Long id, @RequestBody SignRequest req) {
        DepartmentBinding b = findActiveBinding(req.getDeviceCode());
        if (b == null) {
            return ApiResponse.error(401, "设备码未绑定科室");
        }
        WorkOrder wo = workOrderService.getById(id);
        if (!b.getDepartmentId().equals(wo.getDepartmentId())) {
            return ApiResponse.error(403, "无权操作该工单");
        }
        try {
            boolean isDelegate = Boolean.TRUE.equals(req.getIsDelegate());
            return ApiResponse.ok(workOrderService.sign(id, req.getSignerName(), isDelegate,
                    req.getDelegatedName(), req.getSignatureUrl()));
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @PostMapping("/{id}/consumables/pricing")
    public ApiResponse pricing(@PathVariable Long id,
                              @RequestBody PricingRequest req,
                              @RequestHeader(value = "X-Token", required = false) String token) {
        User user = resolveUserByToken(token);
        if (user == null) {
            return ApiResponse.error(401, "未登录或登录已过期");
        }
        if (user.getRole() != UserRole.ADMIN && user.getRole() != UserRole.DISPATCHER) {
            return ApiResponse.error(403, "无权限：只有管理员或调度员可以核价");
        }
        if (req.getConsumables() != null) {
            for (PricingItem item : req.getConsumables()) {
                Consumable c = consumableMapper.selectById(item.getId());
                if (c != null && c.getWorkOrderId().equals(id)) {
                    c.setUnitPrice(item.getUnitPrice());
                    consumableMapper.updateById(c);
                }
            }
        }
        return ApiResponse.ok(workOrderService.listConsumables(id));
    }

    private DepartmentBinding findActiveBinding(String deviceCode) {
        if (deviceCode == null || deviceCode.isBlank()) {
            return null;
        }
        return bindingMapper.selectOne(new LambdaQueryWrapper<DepartmentBinding>()
                .eq(DepartmentBinding::getDeviceCode, deviceCode)
                .eq(DepartmentBinding::getActive, true));
    }

    private User resolveUserByToken(String token) {
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