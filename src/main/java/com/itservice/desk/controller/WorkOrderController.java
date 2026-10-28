package com.itservice.desk.controller;

import com.itservice.desk.security.AuthUserResolver;
import com.itservice.desk.security.DeptTokenService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itservice.desk.dto.*;
import com.itservice.desk.mapper.ConsumableMapper;
import com.itservice.desk.mapper.DepartmentBindingMapper;
import com.itservice.desk.mapper.UserMapper;
import com.itservice.desk.model.*;
import com.itservice.desk.service.WorkOrderAccess;
import com.itservice.desk.service.WorkOrderService;
import org.springframework.web.bind.annotation.*;
import javax.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/api/work-orders")
public class WorkOrderController {

    private final DeptTokenService deptTokenService;

    private final AuthUserResolver authUserResolver;

    private final WorkOrderService workOrderService;
    private final WorkOrderAccess access;
    private final UserMapper userMapper;
    private final DepartmentBindingMapper bindingMapper;
    private final ConsumableMapper consumableMapper;

    public WorkOrderController(WorkOrderService workOrderService,
                                WorkOrderAccess access,
                                UserMapper userMapper,
                                DepartmentBindingMapper bindingMapper,
                                ConsumableMapper consumableMapper,
                                AuthUserResolver authUserResolver,
                                DeptTokenService deptTokenService) {
        this.deptTokenService = deptTokenService;
        this.authUserResolver = authUserResolver;
        this.workOrderService = workOrderService;
        this.access = access;
        this.userMapper = userMapper;
        this.bindingMapper = bindingMapper;
        this.consumableMapper = consumableMapper;
    }

    /**
     * 工单列表（待办三：可选分页）。
     *
     * <p><b>兼容策略</b>：不传 {@code page} 时仍返回数组（老前端
     * {@code state.rows.filter(...)} 不受影响）；传了 {@code page} 才返回分页对象。
     * 这样分页能力是"增量可选"的，不需要前后端同时上线。
     */
    @GetMapping
    public ApiResponse list(@RequestHeader(value = "X-Token", required = false) String token,
                            @RequestParam(required = false) String status,
                            @RequestParam(required = false) Integer page,
                            @RequestParam(required = false) Integer size,
                            @RequestParam(required = false) String keyword) {
        User user = resolveUserByToken(token);
        if (user == null) {
            return ApiResponse.error(401, "未登录或登录已过期");
        }
        if (user.getRole() != UserRole.ADMIN && user.getRole() != UserRole.DISPATCHER) {
            return ApiResponse.error(403, "无权限：只有管理员或调度员可以查看工单列表");
        }
        if (page == null) {
            return ApiResponse.ok(workOrderService.listAll(status));
        }
        long safePage = Math.max(1, page);
        long safeSize = size == null ? 20 : Math.min(Math.max(1, size), 200);
        return ApiResponse.ok(new PageResult<>(
                workOrderService.pageAll(status, keyword, safePage, safeSize)));
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
    public ApiResponse byDepartment(@RequestParam(value = "deptToken", required = false) String deptToken,
                                    @RequestParam(required = false) String deviceCode,
                                    @RequestParam(required = false) String status) {
        Long deptId = resolveDepartmentId(deptToken, deviceCode);
        if (deptId == null) {
            return ApiResponse.error(401, "请选择有效的科室");
        }
        return ApiResponse.ok(workOrderService.listByDepartment(deptId, status));
    }
    @GetMapping("/pending-accept")
    public ApiResponse pendingAccept(@RequestParam(value = "deptToken", required = false) String deptToken,
                                     @RequestParam(required = false) String deviceCode) {
        Long deptId = resolveDepartmentId(deptToken, deviceCode);
        if (deptId == null) {
            return ApiResponse.error(401, "请选择有效的科室");
        }
        return ApiResponse.ok(workOrderService.listPendingAcceptByDepartment(deptId));
    }

    @GetMapping("/{id}")
    public ApiResponse detail(@PathVariable Long id,
                              @RequestHeader(value = "X-Token", required = false) String token,
                              @RequestParam(value = "deptToken", required = false) String deptToken,
                              @RequestParam(required = false) String deviceCode) {
        WorkOrder wo = workOrderService.getById(id);
        // 科室端免登录（按科室做行级过滤）或登录用户（按角色做行级过滤）
        Long deptId = resolveDepartmentId(deptToken, deviceCode);
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
                                     @RequestParam(value = "deptToken", required = false) String deptToken,
                                     @RequestParam(required = false) String deviceCode) {
        WorkOrder wo = workOrderService.getById(id);
        Long deptId = resolveDepartmentId(deptToken, deviceCode);
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
    public ApiResponse sign(@PathVariable Long id, @Valid @RequestBody SignRequest req) {
        Long deptId = resolveDepartmentId(req.getDeptToken(), req.getDeviceCode());
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
            return ApiResponse.error(403, "无权限：只有管理员或调度员在「待核价」状态可以核价");
        }
        // 写入耗材单价
        if (req.getConsumables() != null) {
            for (PricingItem item : req.getConsumables()) {
                Consumable c = consumableMapper.selectById(item.getId());
                if (c != null && c.getWorkOrderId().equals(id)) {
                    c.setUnitPrice(item.getUnitPrice());
                    consumableMapper.updateById(c);
                }
            }
        }
        // 核价完成，推进状态：待核价 → 待签字
        WorkOrder updated = workOrderService.completePricing(id);
        return ApiResponse.ok(updated);
    }

    // 注：工单级「报销」入口已按职责分离移除 —— 报销是财务在「月度报销单」页面对整张报销单执行的动作，
    // 回填后由 ReimbursementService 自动把单内工单推进为已报销，运维侧不再单独操作工单报销。

    private DepartmentBinding findActiveBinding(String deviceCode) {
        if (deviceCode == null || deviceCode.isBlank()) {
            return null;
        }
        return bindingMapper.selectOne(new LambdaQueryWrapper<DepartmentBinding>()
                .eq(DepartmentBinding::getDeviceCode, deviceCode)
                .eq(DepartmentBinding::getActive, true));
    }

    /**
     * 解析科室身份（缺陷 D3 修复）。
     *
     * 修复前：`if (departmentId != null) return departmentId;` —— 直接采信调用方报的数字，
     * 任何人遍历 departmentId=1..N 即可读全院数据（实测 HTTP 200）。
     *
     * 修复后：**只认签名过的科室凭证 deptToken**，departmentId 由签名反解得出，
     * 调用方传什么数字都不再被直接信任。deviceCode 路径保留（内部会查绑定表校验）。
     *
     * @param deptToken 科室凭证（完成设备自助登记后由服务端签发）
     * @param deviceCode 已绑定设备码（旧路径，仍校验 binding 表）
     * @return 可信的 departmentId；无法证明归属时返回 null（调用方应返回 401）
     */
    private Long resolveDepartmentId(String deptToken, String deviceCode) {
        Long fromToken = deptTokenService.verify(deptToken);
        if (fromToken != null) {
            return fromToken;
        }
        DepartmentBinding b = findActiveBinding(deviceCode);
        return b == null ? null : b.getDepartmentId();
    }

    private User resolveUserByToken(String token) {
        return authUserResolver.resolve(token);
    }
}