package com.itservice.desk.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itservice.desk.dto.*;
import com.itservice.desk.mapper.ConsumableMapper;
import com.itservice.desk.mapper.DepartmentBindingMapper;
import com.itservice.desk.mapper.UserMapper;
import com.itservice.desk.model.*;
import com.itservice.desk.service.WorkOrderAccess;
import com.itservice.desk.service.WorkOrderService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/work-orders")
public class WorkOrderController {

    private final WorkOrderService workOrderService;
    private final WorkOrderAccess access;
    private final UserMapper userMapper;
    private final DepartmentBindingMapper bindingMapper;
    private final ConsumableMapper consumableMapper;

    public WorkOrderController(WorkOrderService workOrderService,
                                WorkOrderAccess access,
                                UserMapper userMapper,
                                DepartmentBindingMapper bindingMapper,
                                ConsumableMapper consumableMapper) {
        this.workOrderService = workOrderService;
        this.access = access;
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
        // 今日任务是维修人员专属（管理员/调度员用后台工单列表，财务不接触工单）
        if (user.getRole() != UserRole.REPAIRER) {
            return ApiResponse.error(403, "无权限：今日任务仅维修人员可查看");
        }
        List<WorkOrder> list = workOrderService.listTodayTasks(user.getId());
        return ApiResponse.ok(list);
    }

    @GetMapping("/by-department")
    public ApiResponse byDepartment(@RequestParam(required = false) Long departmentId,
                                    @RequestParam(required = false) String deviceCode,
                                    @RequestParam(required = false) String status) {
        Long deptId = resolveDepartmentId(departmentId, deviceCode);
        if (deptId == null) {
            return ApiResponse.error(401, "请选择有效的科室");
        }
        return ApiResponse.ok(workOrderService.listByDepartment(deptId, status));
    }
    @GetMapping("/pending-accept")
    public ApiResponse pendingAccept(@RequestParam(required = false) Long departmentId,
                                     @RequestParam(required = false) String deviceCode) {
        Long deptId = resolveDepartmentId(departmentId, deviceCode);
        if (deptId == null) {
            return ApiResponse.error(401, "请选择有效的科室");
        }
        return ApiResponse.ok(workOrderService.listPendingAcceptByDepartment(deptId));
    }

    @GetMapping("/{id}")
    public ApiResponse detail(@PathVariable Long id,
                              @RequestHeader(value = "X-Token", required = false) String token,
                              @RequestParam(required = false) Long departmentId,
                              @RequestParam(required = false) String deviceCode) {
        WorkOrder wo = workOrderService.getById(id);
        // 科室端免登录（按科室做行级过滤）或登录用户（按角色做行级过滤）
        Long deptId = resolveDepartmentId(departmentId, deviceCode);
        User user = resolveUserByToken(token);
        if (deptId != null) {
            if (!deptId.equals(wo.getDepartmentId())) {
                return ApiResponse.error(403, "无权查看该工单");
            }
        } else {
            if (user == null) {
                return ApiResponse.error(401, "未登录或登录已过期");
            }
            if (!access.canView(user, wo)) {
                return ApiResponse.error(403, "无权查看该工单");
            }
        }
        // 动态权限由服务端计算后下发，前端按钮据此渲染
        WorkOrderDetail detail = workOrderService.buildDetail(wo);
        detail.setAllowedActions(access.allowedActions(user, deptId, wo));
        return ApiResponse.ok(detail);
    }

    @GetMapping("/{id}/consumables")
    public ApiResponse consumables(@PathVariable Long id,
                                     @RequestHeader(value = "X-Token", required = false) String token,
                                     @RequestParam(required = false) Long departmentId,
                                     @RequestParam(required = false) String deviceCode) {
        WorkOrder wo = workOrderService.getById(id);
        Long deptId = resolveDepartmentId(departmentId, deviceCode);
        if (deptId != null) {
            if (!deptId.equals(wo.getDepartmentId())) {
                return ApiResponse.error(403, "无权查看该工单的耗材记录");
            }
            return ApiResponse.ok(workOrderService.listConsumables(id));
        }
        User user = resolveUserByToken(token);
        if (user == null) {
            return ApiResponse.error(401, "未登录或登录已过期");
        }
        if (!access.canView(user, wo)) {
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
        WorkOrder wo = workOrderService.getById(id);
        if (!access.allowedActions(user, null, wo).contains(WorkOrderAccess.ACCEPT)) {
            return ApiResponse.error(403, "无权限接单：只有被指派的维修人员在「待接单」状态可以接单");
        }
        try {
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
        WorkOrder wo = workOrderService.getById(id);
        if (!access.allowedActions(user, null, wo).contains(WorkOrderAccess.REPAIR)) {
            return ApiResponse.error(403, "无权限提交维修记录：只有被指派的维修人员在「维修中」状态可以提交");
        }
        try {
            return ApiResponse.ok(workOrderService.submitRepair(id, req.getRecord(), req.getConsumables()));
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @PostMapping("/{id}/sign")
    public ApiResponse sign(@PathVariable Long id, @RequestBody SignRequest req) {
        Long deptId = resolveDepartmentId(req.getDepartmentId(), req.getDeviceCode());
        if (deptId == null) {
            return ApiResponse.error(401, "请选择有效的科室");
        }
        WorkOrder wo = workOrderService.getById(id);
        // 科室端签字：仅本科室 + 仅「待签字」状态（状态机动态权限）
        if (!access.allowedActions(null, deptId, wo).contains(WorkOrderAccess.SIGN)) {
            return ApiResponse.error(403, "该工单当前不可签字（仅本科室在「待签字」状态可签字）");
        }
        try {
            boolean isDelegate = Boolean.TRUE.equals(req.getIsDelegate());
            return ApiResponse.ok(workOrderService.sign(id, req.getSignerName(), isDelegate,
                    req.getDelegatedName(), req.getSignatureUrl(), req.getSignType()));
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
        WorkOrder woForPrice = workOrderService.getById(id);
        if (!access.allowedActions(user, null, woForPrice).contains(WorkOrderAccess.PRICE)) {
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

    /** 新方式优先 departmentId；旧方式 deviceCode 兼容 */
    private Long resolveDepartmentId(Long departmentId, String deviceCode) {
        if (departmentId != null) {
            return departmentId;
        }
        DepartmentBinding b = findActiveBinding(deviceCode);
        return b == null ? null : b.getDepartmentId();
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