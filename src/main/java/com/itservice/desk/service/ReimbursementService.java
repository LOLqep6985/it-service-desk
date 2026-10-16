package com.itservice.desk.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itservice.desk.mapper.*;
import com.itservice.desk.model.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
     * 规则（用户 2026-10-16 确认）：当月该科室已存在报销单（含未回填与已回填）→
     *   ① 旧单标记 VOIDED（已作废）② 旧单内工单回退到 PENDING_SIGN（待签字）③ 新单正常生成。
     *   理由：管理员重新点「生成报销单」说明流程要重走（如金额变动/工单调整），
     *   旧单作废留痕、工单回退让它们重新被新单收进去走签字。
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
            List<WorkOrder> deptOrders = entry.getValue();

            // 作废该科室当月所有已存在报销单（未回填的与已回填的都作废）
            List<Reimbursement> existing = reimbursementMapper.selectList(new LambdaQueryWrapper<Reimbursement>()
                    .eq(Reimbursement::getMonth, month)
                    .eq(Reimbursement::getDepartmentId, deptId));
            for (Reimbursement old : existing) {
                if (old.getStatus() != ReimbursementStatus.VOIDED) {
                    old.setStatus(ReimbursementStatus.VOIDED);
                    reimbursementMapper.updateById(old);
                    // 旧单内工单回退到待签字，让它们能被新单收进去重新走签字流程
                    voidWorkOrdersOfReimbursement(old.getId());
                }
            }

            // 新单生成（收进本次所有 PENDING_REIMBURSE 工单 + 刚回退的工单——后者已变 PENDING_SIGN，不会被收）
            // 注：回退后的工单需重新签字才会变回 PENDING_REIMBURSE，再次点生成才会并入。
            //     这里只收仍是 PENDING_REIMBURSE 的工单（即本次触发 generate 的那批）。
            BigDecimal total = calcOrdersTotal(deptOrders);
            Reimbursement r = new Reimbursement();
            r.setMonth(month);
            r.setDepartmentId(deptId);
            r.setTotalAmount(total);
            r.setStatus(ReimbursementStatus.PENDING_SIGN);
            r.setCreatedAt(LocalDateTime.now());
            reimbursementMapper.insert(r);

            for (WorkOrder wo : deptOrders) {
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

    /** 把报销单内的工单从 PENDING_REIMBURSE 回退到 PENDING_SIGN（作废时用） */
    private void voidWorkOrdersOfReimbursement(Long reimbursementId) {
        List<ReimbursementItem> items = reimbursementItemMapper.selectList(
                new LambdaQueryWrapper<ReimbursementItem>()
                        .eq(ReimbursementItem::getReimbursementId, reimbursementId));
        for (ReimbursementItem item : items) {
            WorkOrder wo = workOrderMapper.selectById(item.getWorkOrderId());
            if (wo != null && wo.getStatus() == WorkOrderStatus.PENDING_REIMBURSE) {
                wo.setStatus(WorkOrderStatus.PENDING_SIGN);
                workOrderMapper.updateById(wo);
            }
        }
    }

    /** 重算报销单总额 = 单内全部工单的已核价耗材合计 */
    private void recalcTotal(Reimbursement r) {
        List<ReimbursementItem> items = reimbursementItemMapper.selectList(new LambdaQueryWrapper<ReimbursementItem>()
                .eq(ReimbursementItem::getReimbursementId, r.getId()));
        List<WorkOrder> orders = new ArrayList<>();
        for (ReimbursementItem it : items) {
            WorkOrder wo = workOrderMapper.selectById(it.getWorkOrderId());
            if (wo != null) {
                orders.add(wo);
            }
        }
        r.setTotalAmount(calcOrdersTotal(orders));
        reimbursementMapper.updateById(r);
    }

    /** 一批工单的已核价耗材金额合计 */
    private BigDecimal calcOrdersTotal(List<WorkOrder> orders) {
        BigDecimal total = BigDecimal.ZERO;
        for (WorkOrder wo : orders) {
            List<Consumable> cons = consumableMapper.selectList(new LambdaQueryWrapper<Consumable>()
                    .eq(Consumable::getWorkOrderId, wo.getId()));
            for (Consumable c : cons) {
                if (c.getUnitPrice() != null && c.getQuantity() != null) {
                    total = total.add(c.getUnitPrice().multiply(BigDecimal.valueOf(c.getQuantity())));
                }
            }
        }
        return total;
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

    /**
     * 管理员撤回报销单：仅 PENDING_SIGN（未签字）可撤回 → VOIDED。
     * 单内工单状态不变（仍 PENDING_REIMBURSE），因旧单已 VOIDED 不再算占用，工单自然回到待报销池。
     */
    public Reimbursement revoke(Long id) {
        Reimbursement r = getById(id);
        if (r.getStatus() != ReimbursementStatus.PENDING_SIGN) {
            throw new IllegalArgumentException("只有待护士长确认（未签字）的报销单可以撤回");
        }
        r.setStatus(ReimbursementStatus.VOIDED);
        reimbursementMapper.updateById(r);
        return r;
    }

    /**
     * 护士长退回报销单：仅 PENDING_SIGN 可退回 → RETURNED。
     * 单内工单状态不变（仍 PENDING_REIMBURSE），因旧单已 RETURNED 不再算占用，工单回到待报销池可重新勾选。
     */
    public Reimbursement returnByNurse(Long id) {
        Reimbursement r = getById(id);
        if (r.getStatus() != ReimbursementStatus.PENDING_SIGN) {
            throw new IllegalArgumentException("只有待护士长确认的报销单可以退回");
        }
        r.setStatus(ReimbursementStatus.RETURNED);
        reimbursementMapper.updateById(r);
        return r;
    }

    /**
     * 财务退回报销单：仅 PENDING_REIMBURSE（已签字待审核）可退回 → RETURNED。
     * 单内工单状态不变（仍 PENDING_REIMBURSE，因已签字无需重签），工单回到待报销池可重新勾选生成新单。
     */
    public Reimbursement returnByFinance(Long id) {
        Reimbursement r = getById(id);
        if (r.getStatus() != ReimbursementStatus.PENDING_REIMBURSE) {
            throw new IllegalArgumentException("只有待报销（已签字）的报销单可以退回");
        }
        r.setStatus(ReimbursementStatus.RETURNED);
        reimbursementMapper.updateById(r);
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

    /* ==================== 按科室分池（2026-10-16 报销单设计第 1 步）==================== */

    /**
     * 待报销池摘要：按科室聚合"有待报销工单 或 有未结报销单"的科室，只返回有数据的科室。
     * 每个科室返回：科室名、待报销工单数、各状态报销单数（待签字/待报销/已退回）、最近更新时间。
     */
    public List<Map<String, Object>> poolSummary() {
        LocalDateTime since = LocalDateTime.now().minusDays(RETENTION_DAYS);

        // 1. 有待报销工单的科室 + 数量
        List<WorkOrder> pending = workOrderMapper.selectList(new LambdaQueryWrapper<WorkOrder>()
                .eq(WorkOrder::getStatus, WorkOrderStatus.PENDING_REIMBURSE)
                .orderByAsc(WorkOrder::getDepartmentId));
        Map<Long, Long> pendingCountByDept = new LinkedHashMap<>();
        Map<Long, LocalDateTime> latestByDept = new HashMap<>();
        for (WorkOrder wo : pending) {
            if (wo.getDepartmentId() == null) continue;
            pendingCountByDept.merge(wo.getDepartmentId(), 1L, Long::sum);
            LocalDateTime s = wo.getSignedAt() != null ? wo.getSignedAt() : wo.getCreatedAt();
            if (s != null) {
                latestByDept.merge(wo.getDepartmentId(), s, (a, b) -> a.isAfter(b) ? a : b);
            }
        }

        // 2. 近 6 个月有报销单的科室 + 按状态计数
        List<Reimbursement> reimbs = reimbursementMapper.selectList(new LambdaQueryWrapper<Reimbursement>()
                .ge(Reimbursement::getCreatedAt, since));
        Map<Long, Map<String, Long>> reimbByDept = new HashMap<>();
        for (Reimbursement r : reimbs) {
            if (r.getDepartmentId() == null) continue;
            Map<String, Long> m = reimbByDept.computeIfAbsent(r.getDepartmentId(), k -> new HashMap<>());
            m.merge(r.getStatus().name(), 1L, Long::sum);
            LocalDateTime c = r.getCreatedAt();
            if (c != null) {
                latestByDept.merge(r.getDepartmentId(), c, (a, b) -> a.isAfter(b) ? a : b);
            }
        }

        // 3. 合并：有待报销工单 或 有报销单 的科室才出现
        Set<Long> deptIds = new LinkedHashSet<>();
        deptIds.addAll(pendingCountByDept.keySet());
        deptIds.addAll(reimbByDept.keySet());

        List<Map<String, Object>> result = new ArrayList<>();
        for (Long deptId : deptIds) {
            Department dept = departmentMapper.selectById(deptId);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("departmentId", deptId);
            m.put("departmentName", dept == null ? ("科室#" + deptId) : dept.getName());
            m.put("pendingWorkOrders", pendingCountByDept.getOrDefault(deptId, 0L));
            Map<String, Long> rs = reimbByDept.getOrDefault(deptId, Collections.emptyMap());
            m.put("pendingSignCount", rs.getOrDefault("PENDING_SIGN", 0L));
            m.put("pendingReimburseCount", rs.getOrDefault("PENDING_REIMBURSE", 0L));
            m.put("reimbursedCount", rs.getOrDefault("REIMBURSED", 0L));
            m.put("voidedCount", rs.getOrDefault("VOIDED", 0L));
            m.put("latestAt", latestByDept.get(deptId));
            result.add(m);
        }
        return result;
    }

    /**
     * 从待报销池勾选工单生成报销单（按科室分池模式）。
     * 校验：工单属于指定科室、状态为待报销、未被其他报销单占用。生成后工单被关联进新单（状态机不变，仍 PENDING_REIMBURSE，
     * 由报销单明细记录归属，前端从池中移出展示）。
     */
    @Transactional
    public Reimbursement generateFromPool(Long departmentId, List<Long> workOrderIds, Long operatorId) {
        if (departmentId == null) {
            throw new IllegalArgumentException("请选择科室");
        }
        if (workOrderIds == null || workOrderIds.isEmpty()) {
            throw new IllegalArgumentException("请至少勾选一条待报销工单");
        }

        // 同一科室同一时间最多允许一张"待护士长确认"报销单
        Long pendingSignCount = reimbursementMapper.selectCount(new LambdaQueryWrapper<Reimbursement>()
                .eq(Reimbursement::getDepartmentId, departmentId)
                .eq(Reimbursement::getStatus, ReimbursementStatus.PENDING_SIGN));
        if (pendingSignCount != null && pendingSignCount > 0) {
            throw new IllegalArgumentException("该科室已有待护士长确认的报销单，请先等签字完成或撤回后再生成新单");
        }

        // 校验工单：归属、状态、未被占用
        List<WorkOrder> orders = new ArrayList<>();
        for (Long woId : workOrderIds) {
            WorkOrder wo = workOrderMapper.selectById(woId);
            if (wo == null) {
                throw new IllegalArgumentException("工单 #" + woId + " 不存在");
            }
            if (!departmentId.equals(wo.getDepartmentId())) {
                throw new IllegalArgumentException("工单 #" + woId + " 不属于该科室");
            }
            if (wo.getStatus() != WorkOrderStatus.PENDING_REIMBURSE) {
                throw new IllegalArgumentException("工单 #" + woId + " 不是待报销状态");
            }
            // 占用校验：只算被"活跃报销单"（待签字/待报销/已报销）占用的；
            // 已退回/已作废的旧占用不算——退回/作废后工单重新入池可被勾选
            List<ReimbursementItem> occItems = reimbursementItemMapper.selectList(
                    new LambdaQueryWrapper<ReimbursementItem>().eq(ReimbursementItem::getWorkOrderId, woId));
            for (ReimbursementItem it : occItems) {
                Reimbursement occ = reimbursementMapper.selectById(it.getReimbursementId());
                if (occ != null && occ.getStatus() != ReimbursementStatus.VOIDED
                        && occ.getStatus() != ReimbursementStatus.RETURNED) {
                    throw new IllegalArgumentException("工单 #" + woId + " 已被报销单 #" + occ.getId() + " 占用，不可重复报销");
                }
            }
            orders.add(wo);
        }

        BigDecimal total = calcOrdersTotal(orders);
        Reimbursement r = new Reimbursement();
        r.setMonth(YearMonth.now().toString());  // 勾选模式不强制按月，用当前月份做归属标识
        r.setDepartmentId(departmentId);
        r.setTotalAmount(total);
        r.setStatus(ReimbursementStatus.PENDING_SIGN);
        r.setCreatedAt(LocalDateTime.now());
        reimbursementMapper.insert(r);

        for (WorkOrder wo : orders) {
            ReimbursementItem item = new ReimbursementItem();
            item.setReimbursementId(r.getId());
            item.setWorkOrderId(wo.getId());
            item.setCreatedAt(LocalDateTime.now());
            reimbursementItemMapper.insert(item);
        }
        return r;
    }
}