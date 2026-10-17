package com.itservice.desk.service;

import com.itservice.desk.model.User;
import com.itservice.desk.model.UserRole;
import com.itservice.desk.model.WorkOrder;
import com.itservice.desk.model.WorkOrderStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 工单权限矩阵测试：行级权限（能看哪条）× 状态机动态权限（这条现在能做什么）。
 * 对应 findings.md#D-011 的权限矩阵表。
 */
@ExtendWith(MockitoExtension.class)
class WorkOrderAccessTest {

    @InjectMocks
    private WorkOrderAccess access;

    private static User user(long id, UserRole role) {
        User u = new User();
        u.setId(id);
        u.setRole(role);
        u.setName(role.getLabel() + id);
        return u;
    }

    private static WorkOrder order(WorkOrderStatus status, Long assigneeId, Long deptId, Long creatorId) {
        WorkOrder wo = new WorkOrder();
        wo.setId(100L);
        wo.setStatus(status);
        wo.setAssigneeId(assigneeId);
        wo.setDepartmentId(deptId);
        wo.setCreatorId(creatorId);
        return wo;
    }

    private static List<String> actions(WorkOrderAccess access, User u, Long deptId, WorkOrder wo) {
        return access.allowedActions(u, deptId, wo);
    }

    @Test
    @DisplayName("管理员/调度员：待核价时可核价，其它进行中状态只读")
    void adminAndDispatcherCanPriceWhenPricing() {
        for (UserRole role : Arrays.asList(UserRole.ADMIN, UserRole.DISPATCHER)) {
            User u = user(1L, role);
            // 待核价：VIEW + PRICE
            WorkOrder pricing = order(WorkOrderStatus.PENDING_PRICING, 3L, 13L, 5L);
            assertEquals(Arrays.asList(WorkOrderAccess.VIEW, WorkOrderAccess.PRICE),
                    actions(access, u, null, pricing), role + " 待核价动作应为 VIEW+PRICE");
            // 维修中：仅 VIEW（核价是待核价时才有）
            WorkOrder inProg = order(WorkOrderStatus.IN_PROGRESS, 3L, 13L, 5L);
            assertEquals(Collections.singletonList(WorkOrderAccess.VIEW),
                    actions(access, u, null, inProg), role + " 维修中应仅 VIEW");
        }
    }

    @Test
    @DisplayName("职责分离：管理员/调度员在待报销状态也【不可】报销（报销由财务整单回填）")
    void adminCannotReimburseWorkOrder() {
        for (UserRole role : Arrays.asList(UserRole.ADMIN, UserRole.DISPATCHER)) {
            User u = user(1L, role);
            WorkOrder wo = order(WorkOrderStatus.PENDING_REIMBURSE, 3L, 13L, 5L);
            assertEquals(Collections.singletonList(WorkOrderAccess.VIEW),
                    actions(access, u, null, wo), role + " 待报销动作应仅 VIEW（报销归财务）");
        }
    }

    @Test
    @DisplayName("维修工：待接单时可接单")
    void repairerCanAcceptWhenAssigned() {
        WorkOrder wo = order(WorkOrderStatus.ASSIGNED, 3L, 13L, 5L);
        User u = user(3L, UserRole.REPAIRER);
        assertTrue(access.canView(u, wo));
        assertEquals(Arrays.asList(WorkOrderAccess.VIEW, WorkOrderAccess.ACCEPT), actions(access, u, null, wo));
    }

    @Test
    @DisplayName("维修工：维修中时可提交维修记录")
    void repairerCanRepairWhenInProgress() {
        WorkOrder wo = order(WorkOrderStatus.IN_PROGRESS, 3L, 13L, 5L);
        User u = user(3L, UserRole.REPAIRER);
        assertEquals(Arrays.asList(WorkOrderAccess.VIEW, WorkOrderAccess.REPAIR), actions(access, u, null, wo));
    }

    @Test
    @DisplayName("状态机动态权限：提交维修（待核价）后维修工权限收窄为只读")
    void repairerBecomesReadOnlyAfterSubmit() {
        WorkOrder wo = order(WorkOrderStatus.PENDING_PRICING, 3L, 13L, 5L);
        User u = user(3L, UserRole.REPAIRER);
        assertTrue(access.canView(u, wo));
        assertEquals(Collections.singletonList(WorkOrderAccess.VIEW), actions(access, u, null, wo));
    }

    @Test
    @DisplayName("维修工：看不到也没法操作别人名下的工单")
    void repairerCannotTouchOthersOrder() {
        WorkOrder wo = order(WorkOrderStatus.IN_PROGRESS, 4L, 13L, 5L);
        User other = user(3L, UserRole.REPAIRER);
        assertFalse(access.canView(other, wo));
        assertTrue(actions(access, other, null, wo).isEmpty());
    }

    @Test
    @DisplayName("科室端：本科室待签字工单可签字")
    void departmentCanSignPendingSign() {
        WorkOrder wo = order(WorkOrderStatus.PENDING_SIGN, 3L, 13L, 5L);
        assertEquals(Arrays.asList(WorkOrderAccess.VIEW, WorkOrderAccess.SIGN), actions(access, null, 13L, wo));
    }

    @Test
    @DisplayName("科室端：未到待签字状态时没有签字权（动态权限）")
    void departmentCannotSignBeforePendingSign() {
        for (WorkOrderStatus status : Arrays.asList(WorkOrderStatus.ASSIGNED, WorkOrderStatus.IN_PROGRESS,
                WorkOrderStatus.PENDING_PRICING, WorkOrderStatus.PENDING_REIMBURSE)) {
            WorkOrder wo = order(status, 3L, 13L, 5L);
            assertEquals(Collections.singletonList(WorkOrderAccess.VIEW), actions(access, null, 13L, wo),
                    status + " 时科室端仅可查看");
        }
    }

    @Test
    @DisplayName("科室端与登录用户均无法越权访问其他科室工单")
    void otherDepartmentIsDenied() {
        WorkOrder wo = order(WorkOrderStatus.PENDING_SIGN, 3L, 13L, 5L);
        assertTrue(actions(access, null, 14L, wo).isEmpty(), "别的科室不能看也不能签");
        assertTrue(actions(access, user(9L, UserRole.REPAIRER), null, wo).isEmpty(), "非指派维修工不可见");
    }

    @Test
    @DisplayName("财务：不接触工单（只管报销单）")
    void financeHasNoWorkOrderAccess() {
        WorkOrder wo = order(WorkOrderStatus.IN_PROGRESS, 3L, 13L, 5L);
        User finance = user(6L, UserRole.FINANCE);
        assertFalse(access.canView(finance, wo));
        assertTrue(actions(access, finance, null, wo).isEmpty());
    }

    @Test
    @DisplayName("未登录且无科室上下文：看不到任何动作")
    void anonymousIsDenied() {
        WorkOrder wo = order(WorkOrderStatus.ASSIGNED, 3L, 13L, 5L);
        assertTrue(actions(access, null, null, wo).isEmpty());
        assertFalse(access.canView(null, wo));
    }
}
