package com.itservice.desk.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itservice.desk.dto.ConsumableItem;
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
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class WorkOrderService {

    private final WorkOrderMapper workOrderMapper;
    private final DepartmentMapper departmentMapper;
    private final UserMapper userMapper;
    private final ConsumableMapper consumableMapper;
    private final ReportMapper reportMapper;
    private final ReimbursementItemMapper reimbursementItemMapper;
    private final ReimbursementMapper reimbursementMapper;
    private final NotificationService notificationService;

    public WorkOrderService(WorkOrderMapper workOrderMapper,
                            DepartmentMapper departmentMapper,
                            UserMapper userMapper,
                            ConsumableMapper consumableMapper,
                            ReportMapper reportMapper,
                            ReimbursementItemMapper reimbursementItemMapper,
                            ReimbursementMapper reimbursementMapper,
                            NotificationService notificationService) {
        this.workOrderMapper = workOrderMapper;
        this.departmentMapper = departmentMapper;
        this.userMapper = userMapper;
        this.consumableMapper = consumableMapper;
        this.reportMapper = reportMapper;
        this.reimbursementItemMapper = reimbursementItemMapper;
        this.reimbursementMapper = reimbursementMapper;
        this.notificationService = notificationService;
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

    public List<WorkOrder> listAll(String status) {
        LambdaQueryWrapper<WorkOrder> qw = new LambdaQueryWrapper<WorkOrder>()
                .orderByDesc(WorkOrder::getCreatedAt);
        if (status != null && !status.isBlank()) {
            qw.eq(WorkOrder::getStatus, status);
        }
        return workOrderMapper.selectList(qw);
    }

    /**
     * 分页查询工单（待办三）。
     *
     * <p>与 {@link #listAll(String)} 并存而非替换：老接口保持返回 List，
     * 前端既有代码（{@code state.rows.filter(...)}）不受影响；
     * 新页面可传 page/size 走分页，逐步迁移。
     *
     * <p><b>为什么必须支持 keyword</b>：前端原先是"全量拉取 → 浏览器端搜索"。
     * 一旦改成服务端分页而不同时支持服务端搜索，用户只能搜到当前页的 10 条，
     * 搜索功能会<b>静默退化</b>——不报错，但用户会以为"数据丢了"。
     *
     * @param keyword 模糊匹配工单标题 / 科室名 / 故障描述；空白视为不过滤
     */
    public IPage<WorkOrder> pageAll(String status, String keyword, long page, long size) {
        LambdaQueryWrapper<WorkOrder> qw = new LambdaQueryWrapper<WorkOrder>()
                .orderByDesc(WorkOrder::getCreatedAt);
        if (status != null && !status.isBlank()) {
            qw.eq(WorkOrder::getStatus, status);
        }
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.trim();
            // 用 and(...) 包住 or 条件，避免 OR 污染外层的 status 过滤
            qw.and(w -> w.like(WorkOrder::getTitle, kw)
                    .or().like(WorkOrder::getDepartmentName, kw)
                    .or().like(WorkOrder::getDescription, kw));
        }
        return workOrderMapper.selectPage(new Page<>(page, size), qw);
    }

    /** 分页查询某科室工单（科室端历史记录较多，最需要分页） */
    public IPage<WorkOrder> pageByDepartment(Long departmentId, String status, long page, long size) {
        LambdaQueryWrapper<WorkOrder> qw = new LambdaQueryWrapper<WorkOrder>()
                .eq(WorkOrder::getDepartmentId, departmentId)
                .orderByDesc(WorkOrder::getCreatedAt);
        if (status != null && !status.isBlank()) {
            qw.eq(WorkOrder::getStatus, status);
        }
        return workOrderMapper.selectPage(new Page<>(page, size), qw);
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
            throw new com.itservice.desk.exception.NotFoundException("工单不存在");
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

    /**
     * 签字（待办二：加事务）。
     * 该方法同时改 {@code work_order}（状态/签字字段）与 {@code report}（同步为已完成），
     * 修复前无事务包裹：若第二步失败，工单已变「待报销」但报修单仍是「已派单」，数据不一致。
     */
    @Transactional(rollbackFor = Exception.class)
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
    @Transactional(rollbackFor = Exception.class)
    public WorkOrder submitRepair(Long id, String record, List<ConsumableItem> consumables) {
        WorkOrder wo = getById(id);
        if (wo.getStatus() != WorkOrderStatus.IN_PROGRESS) {
            throw new IllegalArgumentException("只有维修中的工单可以提交关闭");
        }
        wo.setRepairRecord(record);
        // 先落耗材：无耗材则无需管理员核价，直接进入待签字（跳过 PENDING_PRICING）
        boolean hasConsumables = false;
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
                hasConsumables = true;
            }
        }
        // 有耗材 → 待核价（管理员填金额）；无耗材 → 待签字（跳过核价，科室直接验收）
        wo.setStatus(hasConsumables ? WorkOrderStatus.PENDING_PRICING : WorkOrderStatus.PENDING_SIGN);
        workOrderMapper.updateById(wo);
        // 无耗材直接进入待签字 → 事件4：通知报修人设备 + 护士长设备
        if (wo.getStatus() == WorkOrderStatus.PENDING_SIGN) {
            notificationService.notifyPendingSign(wo);
        }
        return wo;
    }

    /**
     * 管理员核价完成：填好金额后提交，工单从待核价 → 待签字。
     * 耗材单价已在 pricing 接口写入；此动作只推进状态。
     */
    @Transactional(rollbackFor = Exception.class)
    public WorkOrder completePricing(Long id) {
        WorkOrder wo = getById(id);
        if (wo.getStatus() != WorkOrderStatus.PENDING_PRICING) {
            throw new IllegalArgumentException("只有待核价的工单可以完成核价");
        }
        wo.setStatus(WorkOrderStatus.PENDING_SIGN);
        workOrderMapper.updateById(wo);
        // 核价完成进入待签字 → 事件4
        notificationService.notifyPendingSign(wo);
        return wo;
    }

    /**
     * 月度报销：把待报销工单推进为已报销。
     * 由 ReimbursementService.reimburse() 在报销单回填时按明细逐条调用。
     */
    @Transactional(rollbackFor = Exception.class)
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