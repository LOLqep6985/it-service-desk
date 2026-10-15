package com.itservice.desk.service;

import com.itservice.desk.mapper.ReportMapper;
import com.itservice.desk.model.Report;
import com.itservice.desk.model.User;
import com.itservice.desk.model.UserRole;
import com.itservice.desk.model.WorkOrder;
import com.itservice.desk.model.WorkOrderStatus;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 工单权限的唯一事实来源：**行级权限（能看哪条）× 状态机动态权限（这条现在能做什么）**。
 *
 * 规则一览（对齐《工单全流程规则.md》）：
 * | 角色            | 可见范围              | 可执行动作                                                    |
 * |-----------------|-----------------------|---------------------------------------------------------------|
 * | 管理员/调度员   | 全部工单              | VIEW + 待核价时 PRICE + 待报销时 REIMBURSE                    |
 * | 维修工          | 仅派给自己的工单      | VIEW + 待接单时 ACCEPT + 维修中时 REPAIR                      |
 * | 报修人          | 仅自己的报修单产生的工单 | VIEW                                                        |
 * | 科室端(免登录)  | 仅本科室工单          | VIEW + 待签字时 SIGN                                          |
 * | 财务            | 不可见工单            | —（只管报销单）                                               |
 *
 * 动态权限示例：维修工被派单后从「只读」变「可接单」，接单后可「提交维修」，
 * 提交后（待签字）他又变回只读 —— 这个收敛完全由状态决定，不需要额外配置。
 *
 * 后续加「挂起/驳回/取消」时，只需在本类补动作与状态约束，接口与前端不用改判断逻辑。
 */
@Component
public class WorkOrderAccess {

    public static final String VIEW = "VIEW";
    public static final String ACCEPT = "ACCEPT";
    public static final String REPAIR = "REPAIR";
    public static final String PRICE = "PRICE";
    public static final String SIGN = "SIGN";
    public static final String REIMBURSE = "REIMBURSE";

    private final ReportMapper reportMapper;

    public WorkOrderAccess(ReportMapper reportMapper) {
        this.reportMapper = reportMapper;
    }

    /** 行级权限：该登录用户能否看到这条工单 */
    public boolean canView(User user, WorkOrder wo) {
        if (user == null || wo == null) {
            return false;
        }
        UserRole role = user.getRole();
        if (role == UserRole.ADMIN || role == UserRole.DISPATCHER) {
            return true;
        }
        if (role == UserRole.REPAIRER) {
            return user.getId().equals(wo.getAssigneeId());
        }
        if (role == UserRole.REPORTER) {
            if (user.getId().equals(wo.getCreatorId())) {
                return true;
            }
            // 报修单派单后生成的工单：按报修单归属判断
            if (wo.getReportId() != null) {
                Report report = reportMapper.selectById(wo.getReportId());
                return report != null && user.getId().equals(report.getCreatorId());
            }
            return false;
        }
        // FINANCE 等角色不接触工单
        return false;
    }

    /**
     * 当前请求上下文下允许的动作集合。
     * departmentId 非空表示「科室端免登录」访问（按科室行级过滤）。
     */
    public List<String> allowedActions(User user, Long departmentId, WorkOrder wo) {
        Set<String> actions = new LinkedHashSet<>();
        if (wo == null) {
            return new ArrayList<>(actions);
        }

        // 科室端：本科室可见，待签字时可签字
        if (departmentId != null && departmentId.equals(wo.getDepartmentId())) {
            actions.add(VIEW);
            if (wo.getStatus() == WorkOrderStatus.PENDING_SIGN) {
                actions.add(SIGN);
            }
            return new ArrayList<>(actions);
        }

        if (user == null || !canView(user, wo)) {
            return new ArrayList<>(actions);
        }

        UserRole role = user.getRole();
        if (role == UserRole.ADMIN || role == UserRole.DISPATCHER) {
            actions.add(VIEW);
            // 核价（待核价时）/ 报销（待报销时）
            if (wo.getStatus() == WorkOrderStatus.PENDING_PRICING) {
                actions.add(PRICE);
            }
            if (wo.getStatus() == WorkOrderStatus.PENDING_REIMBURSE) {
                actions.add(REIMBURSE);
            }
            return new ArrayList<>(actions);
        }
        if (role == UserRole.REPAIRER) {
            actions.add(VIEW);
            if (wo.getStatus() == WorkOrderStatus.ASSIGNED) {
                actions.add(ACCEPT);
            }
            if (wo.getStatus() == WorkOrderStatus.IN_PROGRESS) {
                actions.add(REPAIR);
            }
            return new ArrayList<>(actions);
        }
        actions.add(VIEW);   // 报修人：只读
        return new ArrayList<>(actions);
    }
}
