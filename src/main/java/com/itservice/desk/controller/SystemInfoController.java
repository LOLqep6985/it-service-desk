package com.itservice.desk.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itservice.desk.dto.ApiResponse;
import com.itservice.desk.mapper.DepartmentMapper;
import com.itservice.desk.mapper.ReportMapper;
import com.itservice.desk.mapper.UserMapper;
import com.itservice.desk.mapper.WorkOrderMapper;
import com.itservice.desk.model.Report;
import com.itservice.desk.model.ReportStatus;
import com.itservice.desk.model.User;
import com.itservice.desk.model.UserRole;
import com.itservice.desk.model.WorkOrder;
import com.itservice.desk.model.WorkOrderStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 系统信息（管理后台「系统信息」页）。
 * 只暴露运行环境与数据规模，**不返回任何密码**（数据库密码字段不读取、不返回）。
 */
@RestController
@RequestMapping("/api/system")
public class SystemInfoController {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final long STARTED_AT = System.currentTimeMillis();

    @Value("${spring.application.name:it-service-desk}")
    private String appName;

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    @Value("${server.port:8080}")
    private String serverPort;

    private final Environment environment;
    private final UserMapper userMapper;
    private final DepartmentMapper departmentMapper;
    private final ReportMapper reportMapper;
    private final WorkOrderMapper workOrderMapper;

    public SystemInfoController(Environment environment,
                                UserMapper userMapper,
                                DepartmentMapper departmentMapper,
                                ReportMapper reportMapper,
                                WorkOrderMapper workOrderMapper) {
        this.environment = environment;
        this.userMapper = userMapper;
        this.departmentMapper = departmentMapper;
        this.reportMapper = reportMapper;
        this.workOrderMapper = workOrderMapper;
    }

    @GetMapping("/info")
    public ApiResponse info(@RequestHeader(value = "X-Token", required = false) String token) {
        User user = resolveUser(token);
        if (user == null) {
            return ApiResponse.error(401, "未登录或登录已过期");
        }
        if (user.getRole() != UserRole.ADMIN && user.getRole() != UserRole.DISPATCHER) {
            return ApiResponse.error(403, "无权限：只有管理员或调度员可以查看系统信息");
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("application", appName);
        data.put("version", version());
        data.put("serverPort", serverPort);
        data.put("javaVersion", System.getProperty("java.version"));
        data.put("javaVendor", System.getProperty("java.vendor"));
        data.put("os", System.getProperty("os.name") + " / " + System.getProperty("os.arch"));
        data.put("startedAt", LocalDateTime.ofInstant(Instant.ofEpochMilli(STARTED_AT), ZoneId.systemDefault()).format(TIME_FMT));

        long uptimeMs = System.currentTimeMillis() - STARTED_AT;
        data.put("uptimeMinutes", uptimeMs / 60000L);

        data.put("uploadDir", Paths.get(uploadDir).toAbsolutePath().normalize().toString());
        data.put("dbUrl", environment.getProperty("spring.datasource.url"));
        data.put("dbUsername", environment.getProperty("spring.datasource.username"));

        Map<String, Object> counts = new LinkedHashMap<>();
        counts.put("users", count(userMapper.selectCount(null)));
        counts.put("departments", count(departmentMapper.selectCount(null)));
        counts.put("reports", count(reportMapper.selectCount(null)));
        counts.put("workOrders", count(workOrderMapper.selectCount(null)));
        counts.put("completedWorkOrders", count(workOrderMapper.selectCount(new LambdaQueryWrapper<WorkOrder>()
                .in(WorkOrder::getStatus, WorkOrderStatus.PENDING_REIMBURSE, WorkOrderStatus.REIMBURSED))));
        counts.put("pendingReports", count(reportMapper.selectCount(new LambdaQueryWrapper<Report>()
                .eq(Report::getStatus, ReportStatus.PENDING))));
        data.put("counts", counts);

        return ApiResponse.ok(data);
    }

    private long count(Long v) {
        return v == null ? 0L : v;
    }

    /** 打包运行取 MANIFEST 中的 Implementation-Version；IDE 直接运行时回退到 pom 版本 */
    private String version() {
        Package pkg = getClass().getPackage();
        String v = pkg == null ? null : pkg.getImplementationVersion();
        return (v == null || v.isBlank()) ? "0.1.0" : v;
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
