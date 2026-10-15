package com.itservice.desk.dto;

import com.itservice.desk.model.WorkOrder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 工单详情响应（GET /api/work-orders/{id}）。
 * 结构：工单本体 + 服务端算好的「当前上下文允许的动作」+ 来源报修单号 + 报销进度。
 * 前端按钮据此渲染，不再各自硬编码状态判断（动态权限的下发方式）。
 */
@Data
public class WorkOrderDetail {

    private WorkOrder workOrder;

    /** 当前用户/科室端允许的动作：VIEW / ACCEPT / REPAIR / PRICE / SIGN */
    private List<String> allowedActions = new ArrayList<>();

    /** 来源报修单号（如 R000001）；无来源时为 null */
    private String reportNo;

    /** 报销进度（该工单被纳入的报销单；未纳入为 null） */
    private ReimbursementInfo reimbursement;

    @Data
    public static class ReimbursementInfo {
        private Long id;
        /** YYYY-MM */
        private String month;
        private BigDecimal totalAmount;
        /** PENDING_SIGN / PENDING_REIMBURSE / REIMBURSED */
        private String status;
    }
}
