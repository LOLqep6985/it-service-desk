package com.itservice.desk.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itservice.desk.mapper.*;
import com.itservice.desk.model.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;

@Service
public class ReimbursementService {

    private final ReimbursementMapper reimbursementMapper;
    private final ReimbursementItemMapper reimbursementItemMapper;
    private final WorkOrderMapper workOrderMapper;
    private final ConsumableMapper consumableMapper;
    private final DepartmentMapper departmentMapper;
    private final ReportMapper reportMapper;
    private final WorkOrderService workOrderService;

    public ReimbursementService(ReimbursementMapper reimbursementMapper,
                               ReimbursementItemMapper reimbursementItemMapper,
                               WorkOrderMapper workOrderMapper,
                               ConsumableMapper consumableMapper,
                               DepartmentMapper departmentMapper,
                               ReportMapper reportMapper,
                               WorkOrderService workOrderService) {
        this.reimbursementMapper = reimbursementMapper;
        this.reimbursementItemMapper = reimbursementItemMapper;
        this.workOrderMapper = workOrderMapper;
        this.consumableMapper = consumableMapper;
        this.departmentMapper = departmentMapper;
        this.reportMapper = reportMapper;
        this.workOrderService = workOrderService;
    }

    /**
     * 按月汇总待报销（PENDING_REIMBURSE，即已签字验收）工单生成报销单，按科室分组。
     * 同一科室同一月份只生成一次；返回本次新生成的报销单列表。
     */
    public List<Reimbursement> generate(String month) {
        YearMonth ym;
        try {
            ym = YearMonth.parse(month);
        } catch (Exception e) {
            throw new IllegalArgumentException("月份格式不正确，应为 YYYY-MM");
        }
        LocalDateTime start = ym.atDay(1).atStartOfDay();
        LocalDateTime end = start.plusMonths(1);

        List<WorkOrder> signedOrders = workOrderMapper.selectList(new LambdaQueryWrapper<WorkOrder>()
                .eq(WorkOrder::getStatus, WorkOrderStatus.PENDING_REIMBURSE)
                .ge(WorkOrder::getSignedAt, start)
                .lt(WorkOrder::getSignedAt, end)
                .orderByAsc(WorkOrder::getDepartmentId));

        Map<Long, List<WorkOrder>> byDept = new LinkedHashMap<>();
        for (WorkOrder wo : signedOrders) {
            byDept.computeIfAbsent(wo.getDepartmentId(), k -> new ArrayList<>()).add(wo);
        }

        List<Reimbursement> created = new ArrayList<>();
        for (Map.Entry<Long, List<WorkOrder>> entry : byDept.entrySet()) {
            Long deptId = entry.getKey();
            if (deptId == null) {
                continue;
            }
            Long existed = reimbursementMapper.selectCount(new LambdaQueryWrapper<Reimbursement>()
                    .eq(Reimbursement::getMonth, month)
                    .eq(Reimbursement::getDepartmentId, deptId));
            if (existed != null && existed > 0) {
                continue;
            }
            BigDecimal total = BigDecimal.ZERO;
            for (WorkOrder wo : entry.getValue()) {
                List<Consumable> cons = consumableMapper.selectList(new LambdaQueryWrapper<Consumable>()
                        .eq(Consumable::getWorkOrderId, wo.getId()));
                for (Consumable c : cons) {
                    if (c.getUnitPrice() != null && c.getQuantity() != null) {
                        total = total.add(c.getUnitPrice().multiply(BigDecimal.valueOf(c.getQuantity())));
                    }
                }
            }
            Reimbursement r = new Reimbursement();
            r.setMonth(month);
            r.setDepartmentId(deptId);
            r.setTotalAmount(total);
            r.setStatus(ReimbursementStatus.PENDING_SIGN);
            r.setCreatedAt(LocalDateTime.now());
            reimbursementMapper.insert(r);

            for (WorkOrder wo : entry.getValue()) {
                ReimbursementItem item = new ReimbursementItem();
                item.setReimbursementId(r.getId());
                item.setWorkOrderId(wo.getId());
                item.setCreatedAt(LocalDateTime.now());
                reimbursementItemMapper.insert(item);
            }
            created.add(r);
        }
        return created;
    }

    /** 报销单留存期（天）：仅展示近 6 个月，过期数据不返回（不物理删除，保留可追溯） */
    public static final int RETENTION_DAYS = 180;

    public List<Reimbursement> listAll(String status) {
        LocalDateTime since = LocalDateTime.now().minusDays(RETENTION_DAYS);
        LambdaQueryWrapper<Reimbursement> qw = new LambdaQueryWrapper<Reimbursement>()
                .ge(Reimbursement::getCreatedAt, since)
                .orderByDesc(Reimbursement::getCreatedAt);
        if (status != null && !status.isBlank()) {
            qw.eq(Reimbursement::getStatus, status);
        }
        List<Reimbursement> list = reimbursementMapper.selectList(qw);
        fillDepartmentName(list);
        return list;
    }

    public List<Reimbursement> listByDepartment(Long departmentId) {
        LocalDateTime since = LocalDateTime.now().minusDays(RETENTION_DAYS);
        List<Reimbursement> list = reimbursementMapper.selectList(new LambdaQueryWrapper<Reimbursement>()
                .eq(Reimbursement::getDepartmentId, departmentId)
                .ge(Reimbursement::getCreatedAt, since)
                .orderByDesc(Reimbursement::getCreatedAt));
        fillDepartmentName(list);
        return list;
    }

    private void fillDepartmentName(List<Reimbursement> list) {
        for (Reimbursement r : list) {
            if (r.getDepartmentId() != null) {
                Department dept = departmentMapper.selectById(r.getDepartmentId());
                r.setDepartmentName(dept == null ? null : dept.getName());
            }
        }
    }

    public Reimbursement getById(Long id) {
        Reimbursement r = reimbursementMapper.selectById(id);
        if (r == null) {
            throw new IllegalArgumentException("报销单不存在");
        }
        return r;
    }

    public Map<String, Object> getDetail(Long id) {
        Reimbursement r = getById(id);
        Department dept = departmentMapper.selectById(r.getDepartmentId());
        List<ReimbursementItem> items = reimbursementItemMapper.selectList(new LambdaQueryWrapper<ReimbursementItem>()
                .eq(ReimbursementItem::getReimbursementId, id)
                .orderByAsc(ReimbursementItem::getId));
        List<Long> woIds = new ArrayList<>();
        for (ReimbursementItem it : items) {
            woIds.add(it.getWorkOrderId());
        }
        List<WorkOrder> workOrders = woIds.isEmpty()
                ? Collections.emptyList()
                : workOrderMapper.selectBatchIds(woIds);

        Map<String, Object> data = new HashMap<>();
        data.put("reimbursement", r);
        data.put("departmentName", dept == null ? null : dept.getName());
        data.put("workOrders", workOrders);
        return data;
    }

    public Reimbursement sign(Long id, String signerName, String signatureUrl, String signType) {
        Reimbursement r = getById(id);
        if (r.getStatus() != ReimbursementStatus.PENDING_SIGN) {
            throw new IllegalArgumentException("该报销单不在待签字状态");
        }
        r.setStatus(ReimbursementStatus.PENDING_REIMBURSE);
        r.setSignedBy(signerName);
        r.setSignedAt(LocalDateTime.now());
        r.setSignatureUrl(signatureUrl);      // 手写签名留痕（与工单验收签字一致）
        r.setSignType(SignType.of(signType)); // 签字方式留痕：手写 / 输入姓名生成
        reimbursementMapper.updateById(r);
        return r;
    }

    public Reimbursement reimburse(Long id) {
        Reimbursement r = getById(id);
        if (r.getStatus() != ReimbursementStatus.PENDING_REIMBURSE) {
            throw new IllegalArgumentException("该报销单不在待报销状态");
        }
        r.setStatus(ReimbursementStatus.REIMBURSED);
        r.setReimbursedAt(LocalDateTime.now());
        reimbursementMapper.updateById(r);
        // 财务回填后把报销单内工单推进为「已报销」（工单推进时同步回写报修单为已报销）
        syncWorkOrdersReimbursed(id);
        return r;
    }

    /** 按报销单明细反查工单 → 逐条推进为已报销（幂等：workOrderService.reimburse 只推进待报销状态） */
    private void syncWorkOrdersReimbursed(Long reimbursementId) {
        List<ReimbursementItem> items = reimbursementItemMapper.selectList(
                new LambdaQueryWrapper<ReimbursementItem>()
                        .eq(ReimbursementItem::getReimbursementId, reimbursementId));
        for (ReimbursementItem item : items) {
            WorkOrder wo = workOrderMapper.selectById(item.getWorkOrderId());
            if (wo == null) {
                continue;
            }
            // 只推进待报销工单；已报销或其它状态跳过（防御重复回填）
            if (wo.getStatus() == WorkOrderStatus.PENDING_REIMBURSE) {
                workOrderService.reimburse(wo.getId());
            }
        }
    }
}