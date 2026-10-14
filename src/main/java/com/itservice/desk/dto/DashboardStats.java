package com.itservice.desk.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 管理后台「工作台」概览指标（GET /api/stats/dashboard）。
 *
 * 口径说明：
 * - 待受理 = 待派单：当前流程里「报修单 PENDING → 管理员派单 → 生成工单」没有独立受理环节，
 *   因此这两个指标是同一个数（见 findings.md 中「WorkOrderStatus.PENDING 未被使用」的说明）。
 * - 今日/昨日新增：按 report.created_at 的自然日
 * - 本月报销：按 reimbursement.month = 当前月（YYYY-MM）
 */
@Data
public class DashboardStats {

    /* ---------------- 工单业务指标 ---------------- */

    /** 今日新增报修单数 */
    private long todayReports;
    /** 昨日新增报修单数（供前端做环比展示） */
    private long yesterdayReports;
    /** 待受理（= 待派单）报修单数 */
    private long pendingReports;
    /** 已派单待接单 */
    private long assignedWorkOrders;
    /** 处理中 */
    private long inProgressWorkOrders;
    /** 进行中合计 = 已派单 + 处理中 */
    private long activeWorkOrders;
    /** 待科室验收 */
    private long pendingAcceptWorkOrders;
    /** 本月已完成工单数（按 completed_at 落在本月） */
    private long completedThisMonth;

    /* ---------------- 本月报销 ---------------- */

    /** 统计月份，YYYY-MM */
    private String month;
    /** 本月报销单金额合计 */
    private BigDecimal monthReimbursementAmount = BigDecimal.ZERO;
    /** 本月报销单总数 */
    private long monthReimbursementTotal;
    /** 其中：待签字 */
    private long monthReimbursementPendingSign;
    /** 其中：待报销 */
    private long monthReimbursementPendingReimburse;
    /** 其中：已报销 */
    private long monthReimbursementReimbursed;

    /* ---------------- 近 7 日报修量趋势 ---------------- */

    /** 含今日在内的近 7 天，按日期升序 */
    private List<TrendPoint> reportTrend = new ArrayList<>();

    @Data
    public static class TrendPoint {
        /** 2026-10-08 */
        private String date;
        /** 10/08（用于图表横轴） */
        private String label;
        private long count;

        public TrendPoint() {
        }

        public TrendPoint(String date, String label, long count) {
            this.date = date;
            this.label = label;
            this.count = count;
        }
    }
}
