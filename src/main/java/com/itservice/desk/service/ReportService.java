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

    private final ReportMapper reportMapper;
    private final DepartmentMapper departmentMapper;
    private final WorkOrderMapper workOrderMapper;

    public ReportService(ReportMapper reportMapper,
                          DepartmentMapper departmentMapper,
                          WorkOrderMapper workOrderMapper) {
        this.reportMapper = reportMapper;
        this.departmentMapper = departmentMapper;
        this.workOrderMapper = workOrderMapper;
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
        Report r = new Report();
        r.setDepartmentId(dept.getId());
        r.setDepartmentName(dept.getName());
        r.setReporterName(req.getReporterName());
        r.setDeviceName(req.getDeviceName());
        r.setDevicePhoto(req.getDevicePhoto());
        r.setProblemDesc(req.getProblemDesc());
        r.setDepartmentHead(req.getDepartmentHead());
        r.setStatus(ReportStatus.PENDING);
        r.setCreatorId(creatorId);
        r.setCreatedAt(LocalDateTime.now());
        reportMapper.insert(r);
        r.setReportNo("R" + String.format("%06d", r.getId()));
        reportMapper.updateById(r);
        return r;
    }

    public List<Report> listReportsByDepartment(Long departmentId) {
        return reportMapper.selectList(new LambdaQueryWrapper<Report>()
                .eq(Report::getDepartmentId, departmentId)
                .orderByDesc(Report::getCreatedAt));
    }

    public List<Report> listReports(String status) {
        LambdaQueryWrapper<Report> qw = new LambdaQueryWrapper<Report>()
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
        return wo;
    }
}