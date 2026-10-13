package com.itservice.desk.controller;

import com.itservice.desk.dto.ApiResponse;
import com.itservice.desk.dto.RepairRequest;
import com.itservice.desk.dto.WorkOrderCreateRequest;
import com.itservice.desk.mapper.UserMapper;
import com.itservice.desk.model.User;
import com.itservice.desk.model.UserRole;
import com.itservice.desk.model.WorkOrder;
import com.itservice.desk.service.WorkOrderService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/work-orders")
public class WorkOrderController {

    private final WorkOrderService workOrderService;
    private final UserMapper userMapper;

    public WorkOrderController(WorkOrderService workOrderService, UserMapper userMapper) {
        this.workOrderService = workOrderService;
        this.userMapper = userMapper;
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

    @GetMapping("/{id}")
    public ApiResponse detail(@PathVariable Long id,
                               @RequestHeader(value = "X-Token", required = false) String token) {
        User user = resolveUserByToken(token);
        if (user == null) {
            return ApiResponse.error(401, "未登录或登录已过期");
        }
        try {
            return ApiResponse.ok(workOrderService.getById(id));
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(e.getMessage());
        }
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