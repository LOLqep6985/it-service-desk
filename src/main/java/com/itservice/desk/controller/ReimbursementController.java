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
        if (user.getRole() != UserRole.ADMIN && user.getRole() != UserRole.DISPATCHER) {
            return ApiResponse.error(403, "无权限：只有管理员或调度员可以生成报销单");
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
        if (user.getRole() != UserRole.ADMIN && user.getRole() != UserRole.DISPATCHER) {
            return ApiResponse.error(403, "无权限：只有管理员或调度员可以查看报销单");
        }
        return ApiResponse.ok(reimbursementService.listAll(status));
    }

    /** 科室端免登录：本科室报销单列表 */
    @GetMapping("/by-device")
    public ApiResponse listByDevice(@RequestParam String deviceCode) {
        DepartmentBinding b = findActiveBinding(deviceCode);
        if (b == null) {
            return ApiResponse.error(401, "设备码未绑定科室");
        }
        return ApiResponse.ok(reimbursementService.listByDepartment(b.getDepartmentId()));
    }

    /** 报销单详情（管理员 token 或 科室端 deviceCode） */
    @GetMapping("/{id}")
    public ApiResponse detail(@PathVariable Long id,
                              @RequestHeader(value = "X-Token", required = false) String token,
                              @RequestParam(required = false) String deviceCode) {
        if (deviceCode != null && !deviceCode.isBlank()) {
            DepartmentBinding b = findActiveBinding(deviceCode);
            if (b == null) {
                return ApiResponse.error(401, "设备码未绑定科室");
            }
            Reimbursement r = reimbursementService.getById(id);
            if (!b.getDepartmentId().equals(r.getDepartmentId())) {
                return ApiResponse.error(403, "无权查看该报销单");
            }
            return ApiResponse.ok(reimbursementService.getDetail(id));
        }
        User user = resolveUser(token);
        if (user == null) {
            return ApiResponse.error(401, "未登录或登录已过期");
        }
        if (user.getRole() != UserRole.ADMIN && user.getRole() != UserRole.DISPATCHER) {
            return ApiResponse.error(403, "无权限：只有管理员或调度员可以查看报销单");
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
        DepartmentBinding b = findActiveBinding(req.getDeviceCode());
        if (b == null) {
            return ApiResponse.error(401, "设备码未绑定科室");
        }
        Reimbursement r = reimbursementService.getById(id);
        if (!b.getDepartmentId().equals(r.getDepartmentId())) {
            return ApiResponse.error(403, "无权操作该报销单");
        }
        try {
            return ApiResponse.ok(reimbursementService.sign(id, req.getSignerName(), req.getSignatureUrl()));
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    /** 管理员/调度员：财务回填（标记已报销） */
    @PostMapping("/{id}/reimburse")
    public ApiResponse reimburse(@PathVariable Long id,
                                 @RequestHeader(value = "X-Token", required = false) String token) {
        User user = resolveUser(token);
        if (user == null) {
            return ApiResponse.error(401, "未登录或登录已过期");
        }
        if (user.getRole() != UserRole.ADMIN && user.getRole() != UserRole.DISPATCHER) {
            return ApiResponse.error(403, "无权限：只有管理员或调度员可以操作报销单");
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