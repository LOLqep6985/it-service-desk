package com.itservice.desk.controller;

import com.itservice.desk.dto.ApiResponse;
import com.itservice.desk.dto.ReportCreateRequest;
import com.itservice.desk.mapper.UserMapper;
import com.itservice.desk.model.User;
import com.itservice.desk.model.UserRole;
import com.itservice.desk.service.ReportService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;
    private final UserMapper userMapper;

    public ReportController(ReportService reportService, UserMapper userMapper) {
        this.reportService = reportService;
        this.userMapper = userMapper;
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