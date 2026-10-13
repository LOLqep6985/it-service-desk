package com.itservice.desk.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itservice.desk.dto.AnonymousReportRequest;
import com.itservice.desk.dto.ApiResponse;
import com.itservice.desk.dto.ReportCreateRequest;
import com.itservice.desk.mapper.DepartmentBindingMapper;
import com.itservice.desk.mapper.DepartmentMapper;
import com.itservice.desk.mapper.UserMapper;
import com.itservice.desk.model.Department;
import com.itservice.desk.model.DepartmentBinding;
import com.itservice.desk.model.Report;
import com.itservice.desk.model.ReportStatus;
import com.itservice.desk.model.User;
import com.itservice.desk.model.UserRole;
import com.itservice.desk.service.ReportService;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;
    private final UserMapper userMapper;
    private final DepartmentBindingMapper bindingMapper;
    private final DepartmentMapper departmentMapper;

    public ReportController(ReportService reportService,
                            UserMapper userMapper,
                            DepartmentBindingMapper bindingMapper,
                            DepartmentMapper departmentMapper) {
        this.reportService = reportService;
        this.userMapper = userMapper;
        this.bindingMapper = bindingMapper;
        this.departmentMapper = departmentMapper;
    }

    @PostMapping
    public ApiResponse create(@RequestBody ReportCreateRequest req,
                              @RequestHeader(value = "X-Token", required = false) String token) {
        User user = resolveUser(token);
        if (user == null) {
            return ApiResponse.error(401, "未登录或登录已过期");
        }
        if (user.getRole() != UserRole.REPORTER) {
            return ApiResponse.error(403, "无权限：只有报修人可以提交报修单");
        }
        try {
            return ApiResponse.ok(reportService.createReport(req, user.getId()));
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @PostMapping("/anonymous")
    public ApiResponse createAnonymous(@RequestBody AnonymousReportRequest req) {
        DepartmentBinding b = findActiveBinding(req.getDeviceCode());
        if (b == null) {
            return ApiResponse.error(401, "设备码未绑定科室，无法免登录报修");
        }
        Department dept = departmentMapper.selectById(b.getDepartmentId());
        if (dept == null) {
            return ApiResponse.error("绑定的科室不存在");
        }
        Report recent = reportService.findRecentReport(dept.getId(), req.getDeviceName(), 10);
        if (recent != null && recent.getStatus() != ReportStatus.CANCELLED) {
            return ApiResponse.error("该设备报修已提交，请勿重复提交");
        }
        Report r = new Report();
        r.setDepartmentId(dept.getId());
        r.setDepartmentName(dept.getName());
        r.setReporterName(req.getReporterName());
        r.setDeviceName(req.getDeviceName());
        r.setDevicePhoto(req.getDevicePhoto());
        r.setProblemDesc(req.getProblemDesc());
        r.setDepartmentHead(req.getDepartmentHead() == null || req.getDepartmentHead().isBlank()
                ? dept.getDepartmentHead() : req.getDepartmentHead());
        r.setStatus(ReportStatus.PENDING);
        r.setBindingId(b.getId());
        r.setCreatedAt(LocalDateTime.now());
        reportService.insertReport(r);
        return ApiResponse.ok(r);
    }

    @GetMapping
    public ApiResponse list(@RequestHeader(value = "X-Token", required = false) String token,
                            @RequestParam(required = false) String status) {
        User user = resolveUser(token);
        if (user == null) {
            return ApiResponse.error(401, "未登录或登录已过期");
        }
        if (user.getRole() != UserRole.ADMIN && user.getRole() != UserRole.DISPATCHER) {
            return ApiResponse.error(403, "无权限：只有管理员或调度员可以查看报修单列表");
        }
        return ApiResponse.ok(reportService.listReports(status));
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