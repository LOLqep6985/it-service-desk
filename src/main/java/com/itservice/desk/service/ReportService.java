package com.itservice.desk.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itservice.desk.dto.ReportCreateRequest;
import com.itservice.desk.mapper.DepartmentMapper;
import com.itservice.desk.mapper.ReportMapper;
import com.itservice.desk.mapper.WorkOrderMapper;
import com.itservice.desk.model.*;
import org.springframework.stereotype.Service;

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
    private final com.itservice.desk.mapper.UserMapper userMapper;
    /** 企微群机器人通知：notify.wecom.webhook 未配置时 Bean 不存在，此处可为 null（派单不受影响） */
    private final WecomWebhookNotifier wecomNotifier;
    /** 院区码 → 中文名（通知文案用） */
    private static final java.util.Map<String, String> CAMPUS_LABEL = java.util.Map.of(
            "LAOYUAN", "老院", "XIYUAN", "西院", "DONGYUAN", "东院");

    public ReportService(ReportMapper reportMapper,
                          DepartmentMapper departmentMapper,
                          WorkOrderMapper workOrderMapper,
                          com.itservice.desk.mapper.UserMapper userMapper,
                          org.springframework.beans.factory.ObjectProvider<WecomWebhookNotifier> wecomNotifierProvider) {
        this.reportMapper = reportMapper;
        this.departmentMapper = departmentMapper;
        this.workOrderMapper = workOrderMapper;
        this.userMapper = userMapper;
        this.wecomNotifier = wecomNotifierProvider.getIfAvailable();
    }

    public void insertReport(Report r) {
        reportMapper.insert(r);
        r.setReportNo("R" + String.format("%06d", r.getId()));
        reportMapper.updateById(r);
    }

    public Report createReport(ReportCreateRequest req, Long creatorId) {
        Department dept = departmentMapper.selectById(req.getDepartmentId());
        if (dept == null) {
            throw new IllegalArgumentException("科室不存在");
        }
        String desc = req.getProblemDesc() == null ? "" : req.getProblemDesc();
        Report recent = findDuplicate(dept.getId(), req.getDeviceName(), req.getReporterName(), desc, DUPLICATE_MINUTES);
        if (recent != null) {
            throw new IllegalArgumentException(DUPLICATE_MESSAGE);
        }
        Report r = new Report();
        r.setDepartmentId(dept.getId());
        r.setDepartmentName(dept.getName());
        r.setReporterName(req.getReporterName());
        r.setDeviceName(req.getDeviceName());
        r.setDevicePhoto(req.getDevicePhoto());
        r.setProblemDesc(desc);
        r.setDepartmentHead(req.getDepartmentHead() == null || req.getDepartmentHead().isBlank()
                ? dept.getDepartmentHead() : req.getDepartmentHead());
        r.setStatus(ReportStatus.PENDING);
        r.setCreatorId(creatorId);
        r.setCreatedAt(LocalDateTime.now());
        reportMapper.insert(r);
        r.setReportNo("R" + String.format("%06d", r.getId()));
        reportMapper.updateById(r);
        return r;
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

    public Report getById(Long id) {
        Report r = reportMapper.selectById(id);
        if (r == null) {
            throw new IllegalArgumentException("报修单不存在");
        }
        return r;
    }

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

        // 派单成功后发企微群通知（旁路：失败不影响派单；未配置 webhook 时为空操作）
        if (wecomNotifier != null && wecomNotifier.enabled()) {
            com.itservice.desk.model.User assignee = userMapper.selectById(assigneeId);
            wecomNotifier.notifyDispatch(
                    assignee == null ? "未知" : assignee.getName(),
                    wo.getTitle(),
                    wo.getDepartmentName(),
                    CAMPUS_LABEL.getOrDefault(wo.getCampus(), "院区未记录"),
                    wo.getId());
        }
        return wo;
    }
}