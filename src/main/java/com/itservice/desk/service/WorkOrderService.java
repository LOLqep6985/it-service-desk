package com.itservice.desk.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itservice.desk.dto.ConsumableItem;
import com.itservice.desk.dto.WorkOrderCreateRequest;
import com.itservice.desk.dto.WorkOrderDetail;
import com.itservice.desk.mapper.ConsumableMapper;
import com.itservice.desk.mapper.DepartmentMapper;
import com.itservice.desk.mapper.ReimbursementItemMapper;
import com.itservice.desk.mapper.ReimbursementMapper;
import com.itservice.desk.mapper.ReportMapper;
import com.itservice.desk.mapper.UserMapper;
import com.itservice.desk.mapper.WorkOrderMapper;
import com.itservice.desk.model.*;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class WorkOrderService {

    private final WorkOrderMapper workOrderMapper;
    private final DepartmentMapper departmentMapper;
    private final UserMapper userMapper;
    private final ConsumableMapper consumableMapper;
    private final ReportMapper reportMapper;
    private final ReimbursementItemMapper reimbursementItemMapper;
    private final ReimbursementMapper reimbursementMapper;
    private final AtomicLong assignCursor = new AtomicLong(0);

    public WorkOrderService(WorkOrderMapper workOrderMapper,
                            DepartmentMapper departmentMapper,
                            UserMapper userMapper,
                            ConsumableMapper consumableMapper,
                            ReportMapper reportMapper,
                            ReimbursementItemMapper reimbursementItemMapper,
                            ReimbursementMapper reimbursementMapper) {
        this.workOrderMapper = workOrderMapper;
        this.departmentMapper = departmentMapper;
        this.userMapper = userMapper;
        this.consumableMapper = consumableMapper;
        this.reportMapper = reportMapper;
        this.reimbursementItemMapper = reimbursementItemMapper;
        this.reimbursementMapper = reimbursementMapper;
    }

    /**
     * 组装工单详情：工单本体 + 来源报修单号 + 报销进度。
     * （allowedActions 由调用方通过 WorkOrderAccess 计算后填入）
     */
    public WorkOrderDetail buildDetail(WorkOrder wo) {
        WorkOrderDetail detail = new WorkOrderDetail();
        detail.setWorkOrder(wo);

        if (wo.getReportId() != null) {
            Report report = reportMapper.selectById(wo.getReportId());
            if (report != null && report.getReportNo() != null && !report.getReportNo().isBlank()) {
                detail.setReportNo(report.getReportNo());
            } else {
                detail.setReportNo("R" + String.format("%06d", wo.getReportId()));
            }
        }

        ReimbursementItem item = reimbursementItemMapper.selectOne(new LambdaQueryWrapper<ReimbursementItem>()
                .eq(ReimbursementItem::getWorkOrderId, wo.getId())
                .orderByDesc(ReimbursementItem::getId)
                .last("LIMIT 1"));
        if (item != null && item.getReimbursementId() != null) {
            Reimbursement r = reimbursementMapper.selectById(item.getReimbursementId());
            if (r != null) {
                WorkOrderDetail.ReimbursementInfo info = new WorkOrderDetail.ReimbursementInfo();
                info.setId(r.getId());
                info.setMonth(r.getMonth());
                info.setTotalAmount(r.getTotalAmount());
                info.setStatus(r.getStatus() == null ? null : r.getStatus().name());
                detail.setReimbursement(info);
            }
        }
        return detail;
    }

    public WorkOrder createWorkOrder(WorkOrderCreateRequest req, Long creatorId) {
        Department dept = departmentMapper.selectById(req.getDepartmentId());
        if (dept == null) {
            throw new IllegalArgumentException("科室不存在");
        }
        WorkOrder wo = new WorkOrder();
        wo.setTitle(req.getTitle());
        wo.setDescription(req.getDescription());
        wo.setDepartmentId(dept.getId());
        wo.setDepartmentName(dept.getName());
        wo.setCampus(dept.getCampus());
        wo.setLocation(req.getLocation() == null ? dept.getBuilding() : req.getLocation());
        wo.setContactPerson(req.getContactPerson());
        wo.setContactPhone(req.getContactPhone());
        wo.setPriority(req.getPriority() == null ? "MEDIUM" : req.getPriority());
        wo.setStatus(WorkOrderStatus.ASSIGNED);
        wo.setAssigneeId(nextRepairerId());
        wo.setCreatorId(creatorId);
        wo.setSignStatus(SignStatus.UNSIGNED);
        wo.setCreatedAt(LocalDateTime.now());
        workOrderMapper.insert(wo);
        return wo;
    }

    private Long nextRepairerId() {
        List<User> repairers = userMapper.selectList(new LambdaQueryWrapper<User>()
                .eq(User::getRole, UserRole.REPAIRER)
                .orderByAsc(User::getId));
        if (repairers.isEmpty()) {
            throw new IllegalStateException("没有可用的维修人员");
        }
        long idx = assignCursor.getAndIncrement() % repairers.size();
        return repairers.get((int) idx).getId();
    }

    public List<WorkOrder> listAll(String status) {
        LambdaQueryWrapper<WorkOrder> qw = new LambdaQueryWrapper<WorkOrder>()
                .orderByDesc(WorkOrder::getCreatedAt);
        if (status != null && !status.isBlank()) {
            qw.eq(WorkOrder::getStatus, status);
        }
        return workOrderMapper.selectList(qw);
    }
    public List<WorkOrder> listTodayTasks(Long assigneeId) {
        return workOrderMapper.selectList(new LambdaQueryWrapper<WorkOrder>()
                .eq(WorkOrder::getAssigneeId, assigneeId)
                .in(WorkOrder::getStatus, WorkOrderStatus.ASSIGNED, WorkOrderStatus.IN_PROGRESS)
                .orderByDesc(WorkOrder::getCreatedAt));
    }

    public WorkOrder getById(Long id) {
        WorkOrder wo = workOrderMapper.selectById(id);
        if (wo == null) {
            throw new IllegalArgumentException("工单不存在");
        }
        return wo;
    }

    public WorkOrder accept(Long id) {
        WorkOrder wo = getById(id);
        if (wo.getStatus() != WorkOrderStatus.ASSIGNED) {
            throw new IllegalArgumentException("只有已派单的工单可以接单");
        }
        wo.setStatus(WorkOrderStatus.IN_PROGRESS);
        workOrderMapper.updateById(wo);
        return wo;
    }

    public List<WorkOrder> listByDepartment(Long departmentId, String status) {
        LambdaQueryWrapper<WorkOrder> qw = new LambdaQueryWrapper<WorkOrder>()
                .eq(WorkOrder::getDepartmentId, departmentId)
                .orderByDesc(WorkOrder::getCreatedAt);
        if (status != null && !status.isBlank()) {
            qw.eq(WorkOrder::getStatus, status);
        }
        return workOrderMapper.selectList(qw);
    }
    public List<WorkOrder> listPendingAcceptByDepartment(Long departmentId) {
        return workOrderMapper.selectList(new LambdaQueryWrapper<WorkOrder>()
                .eq(WorkOrder::getDepartmentId, departmentId)
                .eq(WorkOrder::getStatus, WorkOrderStatus.PENDING_SIGN)
                .orderByDesc(WorkOrder::getCreatedAt));
    }

    public WorkOrder sign(Long id, String signerName, boolean isDelegate, String delegatedName,
                          String signatureUrl, String signType) {
        WorkOrder wo = getById(id);
        if (wo.getStatus() != WorkOrderStatus.PENDING_SIGN) {
            throw new IllegalArgumentException("该工单不在待签字状态");
        }
        // 护士长签字验收后 → 待报销（移出工单管理，进入工单历史）
        wo.setStatus(WorkOrderStatus.PENDING_REIMBURSE);
        wo.setCompletedAt(LocalDateTime.now());
        wo.setSignStatus(SignStatus.SIGNED);
        if (isDelegate && delegatedName != null && !delegatedName.isBlank()) {
            wo.setSignedBy(delegatedName + "(由" + signerName + "代签)");
        } else {
            wo.setSignedBy(signerName);
        }
        wo.setSignedAt(LocalDateTime.now());
        wo.setSignatureUrl(signatureUrl);
        wo.setSignType(SignType.of(signType));
        workOrderMapper.updateById(wo);
        // 工单进入待报销（已签字验收）时同步报修单为已完成——报修请求已实质了结
        if (wo.getReportId() != null) {
            Report report = reportMapper.selectById(wo.getReportId());
            if (report != null && report.getStatus() == ReportStatus.DISPATCHED) {
                report.setStatus(ReportStatus.COMPLETED);
                reportMapper.updateById(report);
            }
        }
        return wo;
    }
    public List<Consumable> listConsumables(Long workOrderId) {
        return consumableMapper.selectList(new LambdaQueryWrapper<Consumable>()
                .eq(Consumable::getWorkOrderId, workOrderId)
                .orderByAsc(Consumable::getId));
    }
    public WorkOrder submitRepair(Long id, String record, List<ConsumableItem> consumables) {
        WorkOrder wo = getById(id);
        if (wo.getStatus() != WorkOrderStatus.IN_PROGRESS) {
            throw new IllegalArgumentException("只有维修中的工单可以提交关闭");
        }
        // 维修提交关闭 → 待核价（等待管理员核价填金额）
        wo.setStatus(WorkOrderStatus.PENDING_PRICING);
        wo.setRepairRecord(record);
        workOrderMapper.updateById(wo);
        if (consumables != null) {
            for (ConsumableItem item : consumables) {
                if (item.getName() == null || item.getName().isBlank()) {
                    continue;
                }
                Consumable c = new Consumable();
                c.setWorkOrderId(wo.getId());
                c.setName(item.getName());
                c.setQuantity(item.getQuantity() == null ? 1 : item.getQuantity());
                c.setUnitPrice(item.getUnitPrice());
                c.setIsIou(Boolean.TRUE.equals(item.getIsIou()));
                c.setIouNo(item.getIouNo());
                consumableMapper.insert(c);
            }
        }
        return wo;
    }

    /**
     * 管理员核价完成：填好金额后提交，工单从待核价 → 待签字。
     * 耗材单价已在 pricing 接口写入；此动作只推进状态。
     */
    public WorkOrder completePricing(Long id) {
        WorkOrder wo = getById(id);
        if (wo.getStatus() != WorkOrderStatus.PENDING_PRICING) {
            throw new IllegalArgumentException("只有待核价的工单可以完成核价");
        }
        wo.setStatus(WorkOrderStatus.PENDING_SIGN);
        workOrderMapper.updateById(wo);
        return wo;
    }

    /**
     * 月度报销：把待报销工单推进为已报销。
     * 由 ReimbursementService.reimburse() 在报销单回填时按明细逐条调用。
     */
    public WorkOrder reimburse(Long id) {
        WorkOrder wo = getById(id);
        if (wo.getStatus() != WorkOrderStatus.PENDING_REIMBURSE) {
            throw new IllegalArgumentException("只有待报销的工单可以报销");
        }
        wo.setStatus(WorkOrderStatus.REIMBURSED);
        workOrderMapper.updateById(wo);
        // 工单已报销时同步报修单为已报销——整条报修链路最终闭环
        if (wo.getReportId() != null) {
            Report report = reportMapper.selectById(wo.getReportId());
            if (report != null && report.getStatus() == ReportStatus.COMPLETED) {
                report.setStatus(ReportStatus.REIMBURSED);
                reportMapper.updateById(report);
            }
        }
        return wo;
    }
}