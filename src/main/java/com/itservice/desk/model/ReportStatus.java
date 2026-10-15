package com.itservice.desk.model;

/**
 * 报修单状态。定位是「报修请求当前到了哪一步」，与工单状态机对应：
 *   PENDING(待受理) → DISPATCHED(已派单/处理中) → COMPLETED(已完成)
 *
 * 说明：
 *   - 报修单只表达「请求是否已了结」，不需要工单那种细粒度流转（待接单/维修中/待签字），
 *     工单状态机在 WorkOrderStatus 维护；两者在「已完工」处汇合。
 *   - 派单后 status=DISPATCHED 并保持「处理中」语义，工单签字完成时由 WorkOrderService.sign()
 *     回写为 COMPLETED，使报修历史列表能反映真实终态。
 *   - 后续版本再加：已取消（科室撤回报修）。
 */
public enum ReportStatus {
    PENDING("待受理"),
    DISPATCHED("已派单"),
    COMPLETED("已完成"),
    CANCELLED("已取消");

    private final String label;

    ReportStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}