package com.itservice.desk.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.itservice.desk.dto.DashboardStats;
import com.itservice.desk.mapper.ReimbursementMapper;
import com.itservice.desk.mapper.ReportMapper;
import com.itservice.desk.mapper.WorkOrderMapper;
import com.itservice.desk.model.Reimbursement;
import com.itservice.desk.model.ReimbursementStatus;
import com.itservice.desk.model.Report;
import com.itservice.desk.model.ReportStatus;
import com.itservice.desk.model.WorkOrder;
import com.itservice.desk.model.WorkOrderStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理后台工作台概览统计。
 * 计数/求和在 SQL 侧完成（selectCount / selectMaps 聚合），不把业务列表整表拉回内存。
 */
@Service
public class StatsService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter LABEL_FMT = DateTimeFormatter.ofPattern("MM/dd");
    private static final int TREND_DAYS = 7;

    private final ReportMapper reportMapper;
    private final WorkOrderMapper workOrderMapper;
    private final ReimbursementMapper reimbursementMapper;

    public StatsService(ReportMapper reportMapper,
                        WorkOrderMapper workOrderMapper,
                        ReimbursementMapper reimbursementMapper) {
        this.reportMapper = reportMapper;
        this.workOrderMapper = workOrderMapper;
        this.reimbursementMapper = reimbursementMapper;
    }

    public DashboardStats dashboard() {
        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();
        DashboardStats s = new DashboardStats();

        /* 报修单 */
        s.setTodayReports(countReportsBetween(todayStart, todayStart.plusDays(1)));
        s.setYesterdayReports(countReportsBetween(todayStart.minusDays(1), todayStart));
        s.setPendingReports(countReportsByStatus(ReportStatus.PENDING));

        /* 工单 */
        s.setAssignedWorkOrders(countWorkOrdersByStatus(WorkOrderStatus.ASSIGNED));
        s.setInProgressWorkOrders(countWorkOrdersByStatus(WorkOrderStatus.IN_PROGRESS));
        s.setPendingPricingWorkOrders(countWorkOrdersByStatus(WorkOrderStatus.PENDING_PRICING));
        s.setPendingSignWorkOrders(countWorkOrdersByStatus(WorkOrderStatus.PENDING_SIGN));
        // 进行中 = 工单管理可见的 4 态合计（待接单/维修中/待核价/待签字）
        s.setActiveWorkOrders(s.getAssignedWorkOrders() + s.getInProgressWorkOrders()
                + s.getPendingPricingWorkOrders() + s.getPendingSignWorkOrders());

        YearMonth ym = YearMonth.now();
        LocalDateTime monthStart = ym.atDay(1).atStartOfDay();
        s.setCompletedThisMonth(countCompletedWorkOrdersBetween(monthStart, monthStart.plusMonths(1)));

        /* 本月报销 */
        fillReimbursement(s, ym);

        /* 近 7 日报修量 */
        s.setReportTrend(reportTrend(today));

        return s;
    }

    private long countReportsBetween(LocalDateTime start, LocalDateTime end) {
        Long c = reportMapper.selectCount(new LambdaQueryWrapper<Report>()
                .ge(Report::getCreatedAt, start)
                .lt(Report::getCreatedAt, end));
        return c == null ? 0L : c;
    }

    private long countReportsByStatus(ReportStatus status) {
        Long c = reportMapper.selectCount(new LambdaQueryWrapper<Report>()
                .eq(Report::getStatus, status));
        return c == null ? 0L : c;
    }

    private long countWorkOrdersByStatus(WorkOrderStatus status) {
        Long c = workOrderMapper.selectCount(new LambdaQueryWrapper<WorkOrder>()
                .eq(WorkOrder::getStatus, status));
        return c == null ? 0L : c;
    }

    private long countCompletedWorkOrdersBetween(LocalDateTime start, LocalDateTime end) {
        // 已签字验收 = 待报销 + 已报销（工单完成签字后进入历史）
        Long c = workOrderMapper.selectCount(new LambdaQueryWrapper<WorkOrder>()
                .in(WorkOrder::getStatus, WorkOrderStatus.PENDING_REIMBURSE, WorkOrderStatus.REIMBURSED)
                .ge(WorkOrder::getCompletedAt, start)
                .lt(WorkOrder::getCompletedAt, end));
        return c == null ? 0L : c;
    }

    private void fillReimbursement(DashboardStats s, YearMonth ym) {
        String month = ym.toString();
        List<Reimbursement> list = reimbursementMapper.selectList(new LambdaQueryWrapper<Reimbursement>()
                .eq(Reimbursement::getMonth, month));

        BigDecimal total = BigDecimal.ZERO;
        long pendingSign = 0, pendingReimburse = 0, reimbursed = 0;
        for (Reimbursement r : list) {
            if (r.getTotalAmount() != null) {
                total = total.add(r.getTotalAmount());
            }
            ReimbursementStatus st = r.getStatus();
            if (st == ReimbursementStatus.PENDING_SIGN) {
                pendingSign++;
            } else if (st == ReimbursementStatus.PENDING_REIMBURSE) {
                pendingReimburse++;
            } else if (st == ReimbursementStatus.REIMBURSED) {
                reimbursed++;
            }
        }
        s.setMonth(month);
        s.setMonthReimbursementAmount(total);
        s.setMonthReimbursementTotal(list.size());
        s.setMonthReimbursementPendingSign(pendingSign);
        s.setMonthReimbursementPendingReimburse(pendingReimburse);
        s.setMonthReimbursementReimbursed(reimbursed);
    }

    /** 近 7 日（含今日）每日报修单新增数，SQL 侧按 DATE(created_at) 分组 */
    private List<DashboardStats.TrendPoint> reportTrend(LocalDate today) {
        LocalDate from = today.minusDays(TREND_DAYS - 1L);

        QueryWrapper<Report> qw = new QueryWrapper<>();
        qw.select("DATE(created_at) AS d", "COUNT(*) AS c")
                .ge("created_at", from.atStartOfDay())
                .groupBy("DATE(created_at)");
        List<Map<String, Object>> rows = reportMapper.selectMaps(qw);

        Map<String, Long> byDate = new HashMap<>();
        for (Map<String, Object> row : rows) {
            Object d = row.get("d");
            Object c = row.get("c");
            if (d == null || c == null) {
                continue;
            }
            byDate.put(toDateKey(d), ((Number) c).longValue());
        }

        List<DashboardStats.TrendPoint> trend = new ArrayList<>();
        for (int i = 0; i < TREND_DAYS; i++) {
            LocalDate day = from.plusDays(i);
            String key = day.format(DATE_FMT);
            trend.add(new DashboardStats.TrendPoint(key, day.format(LABEL_FMT), byDate.getOrDefault(key, 0L)));
        }
        return trend;
    }

    /** 兼容不同 JDBC 驱动对 SQL 日期函数返回的类型 */
    private String toDateKey(Object d) {
        if (d instanceof java.sql.Date) {
            return ((java.sql.Date) d).toLocalDate().format(DATE_FMT);
        }
        if (d instanceof java.sql.Timestamp) {
            return ((java.sql.Timestamp) d).toLocalDateTime().toLocalDate().format(DATE_FMT);
        }
        if (d instanceof LocalDate) {
            return ((LocalDate) d).format(DATE_FMT);
        }
        if (d instanceof LocalDateTime) {
            return ((LocalDateTime) d).toLocalDate().format(DATE_FMT);
        }
        return String.valueOf(d);
    }
}
