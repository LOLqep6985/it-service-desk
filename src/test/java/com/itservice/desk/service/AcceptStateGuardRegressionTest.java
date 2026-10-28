package com.itservice.desk.service;

import com.itservice.desk.mapper.ConsumableMapper;
import com.itservice.desk.mapper.DepartmentMapper;
import com.itservice.desk.mapper.ReimbursementItemMapper;
import com.itservice.desk.mapper.ReimbursementMapper;
import com.itservice.desk.mapper.ReportMapper;
import com.itservice.desk.mapper.UserMapper;
import com.itservice.desk.mapper.WorkOrderMapper;
import com.itservice.desk.model.WorkOrder;
import com.itservice.desk.model.WorkOrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 回归测试：锁定 {@code WorkOrderService.accept()} 的<b>状态前置校验</b>。
 *
 * <h2>回归对象（本轮人为注入的变异）</h2>
 * 变异动作：删除 {@code accept()} 中如下三行守卫
 * <pre>
 *   if (wo.getStatus() != WorkOrderStatus.ASSIGNED) {
 *       throw new IllegalArgumentException("只有已派单的工单可以接单");
 *   }
 * </pre>
 *
 * <h2>缺陷后果（若该守卫缺失）</h2>
 * 任何状态的工单都能被"接单"，并被强制改写为 {@code IN_PROGRESS}：
 * <ul>
 *   <li>已报销（REIMBURSED）的工单可被拖回"维修中"，<b>已报销金额与审计链被破坏</b>；</li>
 *   <li>待签字（PENDING_SIGN）工单被重置，护士长已签字的事实被抹掉；</li>
 *   <li>同一工单可被无限次"接单"，维修工时与耗材重复计入。</li>
 * </ul>
 *
 * <h2>为什么必须在这一层测（重要）</h2>
 * HTTP 入口 {@code POST /api/work-orders/{id}/accept} 还有第一道守卫：
 * {@code access.allowedActions(user, null, wo).contains(ACCEPT)}，
 * 而 {@code allowedActions} 仅当 {@code status == ASSIGNED} 时才返回 {@code ACCEPT}。
 * 因此<b>变异无法通过 HTTP 触达</b>——Controller 会先返回 403。
 * 这意味着：<b>只有 Service 层测试才能捕获该变异</b>，Controller 层测试无法代劳。
 * 若将来有其它调用方（定时任务、批处理、新接口）直接调用 {@code accept()}，
 * 这层守卫就是唯一防线。
 *
 * <h2>变异前正确行为（即本测试的预期值）</h2>
 * <ol>
 *   <li>仅 {@code ASSIGNED → IN_PROGRESS} 放行；</li>
 *   <li>其余 5 个状态一律抛 {@code IllegalArgumentException}，消息为「只有已派单的工单可以接单」；</li>
 *   <li>被拒绝时 {@code updateById} <b>零调用</b>（不产生任何 DB 写）；</li>
 *   <li>被拒绝时 <b>传入实体本身也不得被改写</b>（防止"先改内存后抛异常"的隐性污染）。</li>
 * </ol>
 */
class AcceptStateGuardRegressionTest {

    private WorkOrderMapper workOrderMapper;
    private NotificationService notificationService;
    private WorkOrderService service;

    @BeforeEach
    void setUp() {
        workOrderMapper = mock(WorkOrderMapper.class);
        notificationService = mock(NotificationService.class);
        service = new WorkOrderService(
                workOrderMapper,
                mock(DepartmentMapper.class),
                mock(UserMapper.class),
                mock(ConsumableMapper.class),
                mock(ReportMapper.class),
                mock(ReimbursementItemMapper.class),
                mock(ReimbursementMapper.class),
                notificationService);
    }

    private static WorkOrder order(Long id, WorkOrderStatus status) {
        WorkOrder wo = new WorkOrder();
        wo.setId(id);
        wo.setStatus(status);
        wo.setTitle("打印机报修");
        wo.setDepartmentId(6L);
        wo.setAssigneeId(3L);
        return wo;
    }

    @Nested
    @DisplayName("R1 变异点直接回归：非 ASSIGNED 状态必须抛异常")
    class StatusGuard {

        @Test
        @DisplayName("R1-a 逐状态验证：6 个状态中仅 ASSIGNED 放行，其余 5 个必须抛异常")
        void onlyAssignedIsAcceptable() {
            int allowedCount = 0;
            int rejectedCount = 0;

            for (WorkOrderStatus status : WorkOrderStatus.values()) {
                // 每次都必须新实例：Service 会原地修改实体（详见 R4）
                when(workOrderMapper.selectById(anyLong())).thenReturn(order(1L, status));
                if (status == WorkOrderStatus.ASSIGNED) {
                    service.accept(1L);
                    allowedCount++;
                } else {
                    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                            () -> service.accept(1L),
                            "变异捕获点：状态 " + status + " 不应允许接单");
                    assertEquals("只有已派单的工单可以接单", ex.getMessage());
                    rejectedCount++;
                }
            }

            assertEquals(1, allowedCount, "6 个状态中应恰好 1 个（ASSIGNED）可接单");
            assertEquals(5, rejectedCount, "其余 5 个状态必须全部被拒绝");
        }

        @Test
        @DisplayName("R1-b 审计链保护：已报销工单绝不可被接单（最高危场景）")
        void reimbursedOrderCanNeverBeAccepted() {
            WorkOrder wo = order(1L, WorkOrderStatus.REIMBURSED);
            when(workOrderMapper.selectById(1L)).thenReturn(wo);

            assertThrows(IllegalArgumentException.class, () -> service.accept(1L),
                    "已报销工单若可接单，等于把已结算的钱重新拉回维修流程，审计链断裂");

            verify(workOrderMapper, never()).updateById(any(WorkOrder.class));
        }

        @Test
        @DisplayName("R1-c 签字事实保护：待签字工单绝不可被接单（防止抹掉护士长签字）")
        void pendingSignOrderCanNeverBeAccepted() {
            WorkOrder wo = order(1L, WorkOrderStatus.PENDING_SIGN);
            when(workOrderMapper.selectById(1L)).thenReturn(wo);

            assertThrows(IllegalArgumentException.class, () -> service.accept(1L));

            verify(workOrderMapper, never()).updateById(any(WorkOrder.class));
        }

        @Test
        @DisplayName("R1-d 防重复：维修中工单再次接单必须被拒绝（避免工时重复计入）")
        void inProgressCannotBeAcceptedTwice() {
            WorkOrder wo = order(1L, WorkOrderStatus.IN_PROGRESS);
            when(workOrderMapper.selectById(1L)).thenReturn(wo);

            assertThrows(IllegalArgumentException.class, () -> service.accept(1L));

            verify(workOrderMapper, never()).updateById(any(WorkOrder.class));
        }
    }

    @Nested
    @DisplayName("R2 变异点回归：被拒绝时不得产生任何 DB 写")
    class NoWriteOnReject {

        @Test
        @DisplayName("R2 5 个非法状态逐一验证 updateById 零调用（变异后会写入 5 次）")
        void noPersistForAnyIllegalStatus() {
            for (WorkOrderStatus status : WorkOrderStatus.values()) {
                if (status == WorkOrderStatus.ASSIGNED) {
                    continue;
                }
                when(workOrderMapper.selectById(anyLong())).thenReturn(order(1L, status));
                assertThrows(IllegalArgumentException.class, () -> service.accept(1L));
            }
            // 若守卫被删，上述 5 次调用会各写一次库 —— 此处即整体捕获
            verify(workOrderMapper, never()).updateById(any(WorkOrder.class));
        }
    }

    @Nested
    @DisplayName("R3 变异点回归：合法路径的写入值必须精确")
    class LegalPathWriteValue {

        @Test
        @DisplayName("R3 ASSIGNED → 写入值必须为 IN_PROGRESS，且只写一次")
        void assignedWritesInProgressExactlyOnce() {
            when(workOrderMapper.selectById(1L)).thenReturn(order(1L, WorkOrderStatus.ASSIGNED));

            WorkOrder result = service.accept(1L);

            assertEquals(WorkOrderStatus.IN_PROGRESS, result.getStatus());

            ArgumentCaptor<WorkOrder> captor = ArgumentCaptor.forClass(WorkOrder.class);
            verify(workOrderMapper, times(1)).updateById(captor.capture());
            WorkOrder persisted = captor.getValue();
            assertEquals(1L, persisted.getId());
            assertEquals(WorkOrderStatus.IN_PROGRESS, persisted.getStatus());
            // 接单不应顺带篡改派工对象
            assertEquals(3L, persisted.getAssigneeId(), "接单不得改动派工给谁");
        }
    }

    @Nested
    @DisplayName("R4 变异点回归：被拒绝时不得污染内存实体（隐性缺陷）")
    class NoInMemoryCorruption {

        @Test
        @DisplayName("R4 拒绝后传入实体的 status 必须保持原值，不能被顺手改成 IN_PROGRESS")
        void entityStatusUntouchedOnReject() {
            WorkOrder wo = order(1L, WorkOrderStatus.REIMBURSED);
            when(workOrderMapper.selectById(1L)).thenReturn(wo);

            assertThrows(IllegalArgumentException.class, () -> service.accept(1L));

            assertEquals(WorkOrderStatus.REIMBURSED, wo.getStatus(),
                    "即使未落库，也不能把实体状态改成 IN_PROGRESS —— "
                            + "否则同一请求后续逻辑（如组装响应、通知）会误认为已接单");
            assertNotEquals(WorkOrderStatus.IN_PROGRESS, wo.getStatus());
        }
    }

    @Nested
    @DisplayName("R5 变异点回归：工单不存在时的边界（先于状态判断）")
    class NotFoundBoundary {

        @Test
        @DisplayName("R5 不存在的工单：报「工单不存在」，不写库，不误报状态错误")
        void notFoundTakesPrecedenceOverStatusCheck() {
            when(workOrderMapper.selectById(999L)).thenReturn(null);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.accept(999L));

            assertEquals("工单不存在", ex.getMessage(),
                    "边界语义：找不到工单必须是「工单不存在」，"
                            + "不能因为守卫顺序调换而变成「只有已派单的工单可以接单」，否则排查时误导方向");
            verify(workOrderMapper, never()).updateById(any(WorkOrder.class));
        }
    }
}
