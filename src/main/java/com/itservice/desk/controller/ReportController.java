package com.itservice.desk.controller;

import com.itservice.desk.security.AuthUserResolver;
import com.itservice.desk.security.DeptTokenService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itservice.desk.dto.AnonymousReportRequest;
import com.itservice.desk.dto.ApiResponse;
import com.itservice.desk.dto.PageResult;
import com.itservice.desk.mapper.DepartmentBindingMapper;
import com.itservice.desk.mapper.DepartmentMapper;
import com.itservice.desk.mapper.UserMapper;
import com.itservice.desk.model.Department;
import com.itservice.desk.model.DepartmentBinding;
import com.itservice.desk.model.NotificationType;
import com.itservice.desk.model.Report;
import com.itservice.desk.model.ReportStatus;
import com.itservice.desk.model.User;
import com.itservice.desk.model.UserRole;
import com.itservice.desk.service.NotificationService;
import com.itservice.desk.service.ReportService;
import org.springframework.web.bind.annotation.*;
import javax.validation.Valid;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final DeptTokenService deptTokenService;

    private final AuthUserResolver authUserResolver;

    private final ReportService reportService;
    private final UserMapper userMapper;
    private final DepartmentBindingMapper bindingMapper;
    private final DepartmentMapper departmentMapper;
    private final NotificationService notificationService;

    public ReportController(ReportService reportService,
                            UserMapper userMapper,
                            DepartmentBindingMapper bindingMapper,
                            DepartmentMapper departmentMapper,
                            NotificationService notificationService,
                                AuthUserResolver authUserResolver,
                            DeptTokenService deptTokenService) {
        this.deptTokenService = deptTokenService;
        this.authUserResolver = authUserResolver;
        this.reportService = reportService;
        this.userMapper = userMapper;
        this.bindingMapper = bindingMapper;
        this.departmentMapper = departmentMapper;
        this.notificationService = notificationService;
    }

    @PostMapping("/anonymous")
    public ApiResponse createAnonymous(@Valid @RequestBody AnonymousReportRequest req) {
        Department dept = resolveDepartment(req.getDeptToken(), req.getDeviceCode());
        if (dept == null) {
            return ApiResponse.error(401, "请选择有效的科室");
        }
        String desc = req.getProblemDesc() == null ? "" : req.getProblemDesc();
        Report recent = reportService.findDuplicate(dept.getId(), req.getDeviceName(), req.getReporterName(),
                desc, ReportService.DUPLICATE_MINUTES);
        if (recent != null) {
            return ApiResponse.error(ReportService.DUPLICATE_MESSAGE);
        }
        Report r = new Report();
        r.setDepartmentId(dept.getId());
        r.setDepartmentName(dept.getName());
        r.setReporterName(req.getReporterName());
        r.setDeviceName(req.getDeviceName());
        r.setProblemDesc(desc);
        r.setDepartmentHead(req.getDepartmentHead() == null || req.getDepartmentHead().isBlank()
                ? dept.getDepartmentHead() : req.getDepartmentHead());
        r.setStatus(ReportStatus.PENDING);
        r.setBindingId(null);
        r.setClientDeviceId(req.getDeviceId());
        r.setCreatedAt(LocalDateTime.now());
        reportService.insertReport(r);

        // 事件1：报修单提交 → 管理员/调度（旁路，失败不影响报修）
        notificationService.notifyAdmins(NotificationType.REPORT_CREATED,
                "REPORT:" + r.getId(),
                "新报修单：" + (r.getDepartmentName() == null ? "" : r.getDepartmentName()),
                (r.getReporterName() == null ? "" : r.getReporterName()) + " 报修「"
                        + (r.getDeviceName() == null ? "" : r.getDeviceName()) + "」，请及时受理",
                NotificationService.LEVEL_URGENT, null, r.getId());
        return ApiResponse.ok(r);
    }

    @GetMapping
    public ApiResponse list(@RequestHeader(value = "X-Token", required = false) String token,
                            @RequestParam(required = false) String status,
                            @RequestParam(required = false) Integer page,
                            @RequestParam(required = false) Integer size) {
        User user = resolveUser(token);
        if (user == null) {
            return ApiResponse.error(401, "未登录或登录已过期");
        }
        if (user.getRole() != UserRole.ADMIN && user.getRole() != UserRole.DISPATCHER) {
            return ApiResponse.error(403, "无权限：只有管理员或调度员可以查看报修单列表");
        }
        // 不传 page 保持老行为（返回数组）；传了才返回分页对象
        if (page == null) {
            return ApiResponse.ok(reportService.listReports(status));
        }
        long safePage = Math.max(1, page);
        long safeSize = size == null ? 20 : Math.min(Math.max(1, size), 200);
        return ApiResponse.ok(new PageResult<>(reportService.pageReports(status, safePage, safeSize)));
    }

    @GetMapping("/{id}")
    public ApiResponse detail(@PathVariable Long id,
                              @RequestHeader(value = "X-Token", required = false) String token) {
        User user = resolveUser(token);
        if (user == null) {
            return ApiResponse.error(401, "未登录或登录已过期");
        }
        if (user.getRole() != UserRole.ADMIN && user.getRole() != UserRole.DISPATCHER) {
            return ApiResponse.error(403, "无权限：只有管理员或调度员可以查看报修单详情");
        }
        try {
            return ApiResponse.ok(reportService.getById(id));
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(e.getMessage());
        }
    }
    @GetMapping("/by-device")
    public ApiResponse listByDevice(@RequestParam String deviceCode) {
        DepartmentBinding b = findActiveBinding(deviceCode);
        if (b == null) {
            return ApiResponse.error(401, "设备码未绑定科室");
        }
        List<Report> list = reportService.listReportsByDepartment(b.getDepartmentId());
        return ApiResponse.ok(list);
    }

    @GetMapping("/by-department")
    public ApiResponse listByDepartment(@RequestParam Long departmentId) {
        if (departmentId == null) {
            return ApiResponse.error("缺少科室参数");
        }
        return ApiResponse.ok(reportService.listReportsByDepartment(departmentId));
    }

    @PostMapping("/{id}/dispatch")
    public ApiResponse dispatch(@PathVariable Long id,
                                @RequestParam Long assigneeId,
                                @RequestHeader(value = "X-Token", required = false) String token) {
        User user = resolveUser(token);
        if (user == null) {
            return ApiResponse.error(401, "未登录或登录已过期");
        }
        if (user.getRole() != UserRole.ADMIN && user.getRole() != UserRole.DISPATCHER) {
            return ApiResponse.error(403, "无权限：只有管理员或调度员可以派单");
        }
        try {
            return ApiResponse.ok(reportService.dispatch(id, assigneeId, user.getId()));
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

    /**
     * 解析科室（缺陷 D3 修复）：只认签名科室凭证，或已绑定 deviceCode。
     * 修复前 `if (departmentId != null) return departmentMapper.selectById(departmentId);`
     * 会直接采信调用方报的数字，导致任意科室数据可被遍历读取。
     */
    private Department resolveDepartment(String deptToken, String deviceCode) {
        Long fromToken = deptTokenService.verify(deptToken);
        if (fromToken != null) {
            return departmentMapper.selectById(fromToken);
        }
        DepartmentBinding b = findActiveBinding(deviceCode);
        return b == null ? null : departmentMapper.selectById(b.getDepartmentId());
    }

    private User resolveUser(String token) {
        return authUserResolver.resolve(token);
    }
}