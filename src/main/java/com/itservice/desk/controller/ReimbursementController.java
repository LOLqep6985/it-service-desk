package com.itservice.desk.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itservice.desk.dto.ApiResponse;
import com.itservice.desk.dto.ReimbursementSignRequest;
import com.itservice.desk.mapper.DepartmentBindingMapper;
import com.itservice.desk.mapper.UserMapper;
import com.itservice.desk.model.DepartmentBinding;
import com.itservice.desk.model.Reimbursement;
import com.itservice.desk.model.User;
import com.itservice.desk.model.UserRole;
import com.itservice.desk.service.ReimbursementService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reimbursements")
public class ReimbursementController {

    private final ReimbursementService reimbursementService;
    private final UserMapper userMapper;
    private final DepartmentBindingMapper bindingMapper;

    public ReimbursementController(ReimbursementService reimbursementService,
                                   UserMapper userMapper,
                                   DepartmentBindingMapper bindingMapper) {
        this.reimbursementService = reimbursementService;
        this.userMapper = userMapper;
        this.bindingMapper = bindingMapper;
    }

    /** 管理员/调度员：按月生成报销单（汇总已签字工单 + 耗材金额） */
    @PostMapping("/generate")
    public ApiResponse generate(@RequestParam String month,
                                @RequestHeader(value = "X-Token", required = false) String token) {
        User user = resolveUser(token);
        if (user == null) {
            return ApiResponse.error(401, "未登录或登录已过期");
        }
        if (!isOpsStaff(user)) {
            return ApiResponse.error(403, "无权限：只有管理员或调度员可以生成报销单（财务负责回填，不参与生成）");
        }
        try {
            List<Reimbursement> created = reimbursementService.generate(month);
            return ApiResponse.ok(created);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    /** 管理员/调度员：报销单列表 */
    @GetMapping
    public ApiResponse list(@RequestHeader(value = "X-Token", required = false) String token,
                            @RequestParam(required = false) String status) {
        User user = resolveUser(token);
        if (user == null) {
            return ApiResponse.error(401, "未登录或登录已过期");
        }
        if (!isReimbursementStaff(user)) {
            return ApiResponse.error(403, "无权限：只有管理员、调度员或财务可以查看报销单");
        }
        return ApiResponse.ok(reimbursementService.listAll(status));
    }

    /** 科室端免登录：本科室报销单列表 */
    @GetMapping("/by-department")
    public ApiResponse listByDepartment(@RequestParam Long departmentId) {
        if (departmentId == null) {
            return ApiResponse.error("缺少科室参数");
        }
        return ApiResponse.ok(reimbursementService.listByDepartment(departmentId));
    }

    /** 兼容旧设备码方式 */
    @GetMapping("/by-device")
    public ApiResponse listByDevice(@RequestParam(required = false) Long departmentId,
                                    @RequestParam(required = false) String deviceCode) {
        Long deptId = resolveDepartmentId(departmentId, deviceCode);
        if (deptId == null) {
            return ApiResponse.error(401, "请选择有效的科室");
        }
        return ApiResponse.ok(reimbursementService.listByDepartment(deptId));
    }

    /** 报销单详情（管理员 token 或 科室端 departmentId/deviceCode） */
    @GetMapping("/{id}")
    public ApiResponse detail(@PathVariable Long id,
                              @RequestHeader(value = "X-Token", required = false) String token,
                              @RequestParam(required = false) Long departmentId,
                              @RequestParam(required = false) String deviceCode) {
        Long deptId = resolveDepartmentId(departmentId, deviceCode);
        if (deptId != null) {
            Reimbursement r = reimbursementService.getById(id);
            if (!deptId.equals(r.getDepartmentId())) {
                return ApiResponse.error(403, "无权查看该报销单");
            }
            return ApiResponse.ok(reimbursementService.getDetail(id));
        }
        User user = resolveUser(token);
        if (user == null) {
            return ApiResponse.error(401, "未登录或登录已过期");
        }
        if (!isReimbursementStaff(user)) {
            return ApiResponse.error(403, "无权限：只有管理员、调度员或财务可以查看报销单");
        }
        try {
            return ApiResponse.ok(reimbursementService.getDetail(id));
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    /** 科室端免登录：护士长/主任签字确认报销单 */
    @PostMapping("/{id}/sign")
    public ApiResponse sign(@PathVariable Long id, @RequestBody ReimbursementSignRequest req) {
        Long deptId = resolveDepartmentId(req.getDepartmentId(), req.getDeviceCode());
        if (deptId == null) {
            return ApiResponse.error(401, "请选择有效的科室");
        }
        Reimbursement r = reimbursementService.getById(id);
        if (!deptId.equals(r.getDepartmentId())) {
            return ApiResponse.error(403, "无权操作该报销单");
        }
        try {
            return ApiResponse.ok(reimbursementService.sign(id, req.getSignerName(), req.getSignatureUrl(), req.getSignType()));
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    /** 财务回填（资金动作）：**仅财务**可操作；回填后报销单与单内工单、报修单状态同步推进 */
    @PostMapping("/{id}/reimburse")
    public ApiResponse reimburse(@PathVariable Long id,
                                 @RequestHeader(value = "X-Token", required = false) String token) {
        User user = resolveUser(token);
        if (user == null) {
            return ApiResponse.error(401, "未登录或登录已过期");
        }
        if (!isFinance(user)) {
            return ApiResponse.error(403, "无权限：财务回填由财务岗位操作，管理员/调度员只负责生成报销单");
        }
        try {
            return ApiResponse.ok(reimbursementService.reimburse(id));
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(e.getMessage());
        }
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

    /**
     * 报销单的管理端角色：管理员 / 调度员 / 财务。
     * 职责分离（见 findings D-011 / D-022）：
     *   - 生成报销单：运维侧（管理员/调度员）按月汇总，财务不参与生成
     *   - 财务回填：仅财务可操作（资金动作，运维侧不得代填）
     *   - 查看列表/详情：三种角色都可（财务需要看单，运维需要看进度）
     */
    private boolean isReimbursementStaff(User user) {
        UserRole role = user.getRole();
        return role == UserRole.ADMIN || role == UserRole.DISPATCHER || role == UserRole.FINANCE;
    }

    /** 运维侧（管理员/调度员）：负责生成报销单 */
    private boolean isOpsStaff(User user) {
        UserRole role = user.getRole();
        return role == UserRole.ADMIN || role == UserRole.DISPATCHER;
    }

    /** 财务：唯一可执行「财务回填」的角色 */
    private boolean isFinance(User user) {
        return user.getRole() == UserRole.FINANCE;
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