package com.itservice.desk.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itservice.desk.mapper.DepartmentMapper;
import com.itservice.desk.mapper.ReportMapper;
import com.itservice.desk.mapper.WorkOrderMapper;
import com.itservice.desk.model.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReportService {

    /** 重复提交判定窗口（分钟）：只拦「短时间内完全相同的报修」，不影响同科室报修多台设备 */
    public static final int DUPLICATE_MINUTES = 3;
    public static final String DUPLICATE_MESSAGE =
            "相同报修刚刚已提交（同科室·同设备·同报修人·同描述），请勿重复提交；如报修其他设备请修改设备或描述";

    private final ReportMapper reportMapper;
    private final DepartmentMapper departmentMapper;
    private final WorkOrderMapper workOrderMapper;
    private final NotificationService notificationService;

    public ReportService(ReportMapper reportMapper,
                          DepartmentMapper departmentMapper,
                          WorkOrderMapper workOrderMapper,
                          NotificationService notificationService) {
        this.reportMapper = reportMapper;
        this.departmentMapper = departmentMapper;
        this.workOrderMapper = workOrderMapper;
        this.notificationService = notificationService;
    }

    public void insertReport(Report r) {
        reportMapper.insert(r);
        r.setReportNo("R" + String.format("%06d", r.getId()));
        reportMapper.updateById(r);
    }

    /**
     * 防重复提交：只有「同科室 + 同设备 + 同报修人 + 同问题描述」在窗口期内再次提交才算重复。
     * 口径说明：设备名已是宽泛下拉（打印机/电脑…），若只按「科室+设备」判定，
     * 同科室短时间报修两台不同打印机会被误判，因此纳入报修人与问题描述。
     */
    public Report findDuplicate(Long departmentId, String deviceName, String reporterName, String problemDesc, int minutes) {
        if (deviceName == null || deviceName.isBlank()) {
            return null;
        }
        LocalDateTime since = LocalDateTime.now().minusMinutes(minutes);
        return reportMapper.selectOne(new LambdaQueryWrapper<Report>()
                .eq(Report::getDepartmentId, departmentId)
                .eq(Report::getDeviceName, deviceName)
                .eq(Report::getReporterName, reporterName == null ? "" : reporterName)
                .eq(Report::getProblemDesc, problemDesc == null ? "" : problemDesc)
                .ne(Report::getStatus, ReportStatus.CANCELLED)
                .ge(Report::getCreatedAt, since)
                .orderByDesc(Report::getCreatedAt)
                .last("LIMIT 1"));
    }

    /** 报修记录留存期（天）：仅展示近 2 个月，过期数据不返回（不物理删除，保留可追溯） */
    public static final int RETENTION_DAYS = 60;

    public List<Report> listReportsByDepartment(Long departmentId) {
        LocalDateTime since = LocalDateTime.now().minusDays(RETENTION_DAYS);
        return reportMapper.selectList(new LambdaQueryWrapper<Report>()
                .eq(Report::getDepartmentId, departmentId)
                .ge(Report::getCreatedAt, since)
                .orderByDesc(Report::getCreatedAt));
    }

    public List<Report> listReports(String status) {
        LocalDateTime since = LocalDateTime.now().minusDays(RETENTION_DAYS);
        LambdaQueryWrapper<Report> qw = new LambdaQueryWrapper<Report>()
                .ge(Report::getCreatedAt, since)
                .orderByDesc(Report::getCreatedAt);
        if (status != null && !status.isBlank()) {
            qw.eq(Report::getStatus, status);
        }
        return reportMapper.selectList(qw);
    }

    /**
     * 分页查询报修单（待办三扩展）。
     * 与 {@link #listReports(String)} 并存：不传 page 走老接口返回 List，传了才走这里。
     * 保留 60 天留存过滤口径，避免分页把历史数据"翻"出来。
     */
    public IPage<Report> pageReports(String status, long page, long size) {
        LocalDateTime since = LocalDateTime.now().minusDays(RETENTION_DAYS);
        LambdaQueryWrapper<Report> qw = new LambdaQueryWrapper<Report>()
                .ge(Report::getCreatedAt, since)
                .orderByDesc(Report::getCreatedAt);
        if (status != null && !status.isBlank()) {
            qw.eq(Report::getStatus, status);
        }
        return reportMapper.selectPage(new Page<>(page, size), qw);
    }

    /** 分页查询某科室报修单（科室端历史记录） */
    public IPage<Report> pageReportsByDepartment(Long departmentId, long page, long size) {
        LocalDateTime since = LocalDateTime.now().minusDays(RETENTION_DAYS);
        LambdaQueryWrapper<Report> qw = new LambdaQueryWrapper<Report>()
                .eq(Report::getDepartmentId, departmentId)
                .ge(Report::getCreatedAt, since)
                .orderByDesc(Report::getCreatedAt);
        return reportMapper.selectPage(new Page<>(page, size), qw);
    }
    public Report getById(Long id) {
        Report r = reportMapper.selectById(id);
        if (r == null) {
            throw new com.itservice.desk.exception.NotFoundException("报修单不存在");
        }
        return r;
    }

    /**
     * 派单（待办二：加事务）。
     * 插入 {@code work_order} + 更新 {@code report} 状态必须原子：
     * 修复前若更新 report 失败，会留下「已生成工单但报修单仍是待受理」的孤儿数据，
     * 管理员会重复派单。
     */
    @Transactional(rollbackFor = Exception.class)
    public WorkOrder dispatch(Long reportId, Long assigneeId, Long dispatcherId) {
        Report r = getById(reportId);
        if (r.getStatus() != ReportStatus.PENDING) {
            throw new IllegalArgumentException("只有待受理的报修单可以派单");
        }
        Department dept = departmentMapper.selectById(r.getDepartmentId());
        WorkOrder wo = new WorkOrder();
        wo.setTitle(r.getDeviceName() + "报修");
        wo.setDescription(r.getProblemDesc());
        wo.setDepartmentId(r.getDepartmentId());
        wo.setDepartmentName(r.getDepartmentName());
        wo.setCampus(dept == null ? null : dept.getCampus());
        wo.setLocation(dept == null ? null : dept.getBuilding());
        wo.setContactPerson(r.getDepartmentHead());
        wo.setPriority("MEDIUM");
        wo.setStatus(WorkOrderStatus.ASSIGNED);
        wo.setAssigneeId(assigneeId);
        wo.setCreatorId(dispatcherId);
        wo.setReportId(r.getId());
        wo.setSignStatus(SignStatus.UNSIGNED);
        wo.setCreatedAt(LocalDateTime.now());
        workOrderMapper.insert(wo);

        r.setStatus(ReportStatus.DISPATCHED);
        reportMapper.updateById(r);

        // 事件2：派单 → 维修工（旁路，失败不影响派单）
        notificationService.notifyUser(assigneeId, NotificationType.DISPATCHED,
                "DISPATCH:" + wo.getId(),
                "新工单：" + (wo.getTitle() == null ? "" : wo.getTitle()),
                (wo.getDepartmentName() == null ? "" : wo.getDepartmentName()) + "，请及时接单",
                NotificationService.LEVEL_URGENT, wo.getId(), r.getId());

        return wo;
    }
}