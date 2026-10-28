package com.itservice.desk.service;

import com.itservice.desk.dto.ConsumableItem;
import com.itservice.desk.mapper.ConsumableMapper;
import com.itservice.desk.mapper.DepartmentMapper;
import com.itservice.desk.mapper.ReimbursementItemMapper;
import com.itservice.desk.mapper.ReimbursementMapper;
import com.itservice.desk.mapper.ReportMapper;
import com.itservice.desk.mapper.UserMapper;
import com.itservice.desk.mapper.WorkOrderMapper;
import com.itservice.desk.model.Consumable;
import com.itservice.desk.model.WorkOrder;
import com.itservice.desk.model.WorkOrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.mockito.ArgumentCaptor;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 工单状态机「落库级」单元测试：直接驱动 {@link WorkOrderService} 的真实状态迁移方法，
 * 校验「非法流转是否被拒绝」与「状态写入值是否正确」。
 *
 * <p>与已有 {@link WorkOrderAccessTest} 的分工：
 * <ul>
 *   <li>WorkOrderAccessTest 测的是「这个角色此刻能做什么」（纯规则、不碰数据库）</li>
 *   <li>本类测的是「真的执行动作后，状态是否按预期落库」（方法级 + Mock Mapper 捕获写入值）</li>
 * </ul>
 *
 * <p>不连数据库：Mapper 全部 Mock，用 {@link ArgumentCaptor} 捕获 updateById 的实际参数，
 * 因此无需 H2/Testcontainers，也不污染真实库。
 */
class WorkOrderStateMachineTest {

    private WorkOrderMapper workOrderMapper;
    private NotificationService notificationService;
    private ConsumableMapper consumableMapper;
    private WorkOrderService service;

    @BeforeEach
    void setUp() {
        workOrderMapper = mock(WorkOrderMapper.class);
        notificationService = mock(NotificationService.class);
        consumableMapper = mock(ConsumableMapper.class);
        service = new WorkOrderService(
                workOrderMapper,
                mock(DepartmentMapper.class),
                mock(UserMapper.class),
                consumableMapper,
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

    /** 捕获真正写进 DB 的那份工单 */
    private WorkOrder capturedUpdate() {
        ArgumentCaptor<WorkOrder> captor = ArgumentCaptor.forClass(WorkOrder.class);
        verify(workOrderMapper, times(1)).updateById(captor.capture());
        return captor.getValue();
    }

    @Nested
    @DisplayName("接单 accept()：只允许 ASSIGNED → IN_PROGRESS")
    class Accept {

        @Test
        @DisplayName("合法：待接单 → 维修中，且写入值确为 IN_PROGRESS")
        void assignedToInProgress() {
            when(workOrderMapper.selectById(1L)).thenReturn(order(1L, WorkOrderStatus.ASSIGNED));

            WorkOrder result = service.accept(1L);

            assertEquals(WorkOrderStatus.IN_PROGRESS, result.getStatus());
            assertEquals(WorkOrderStatus.IN_PROGRESS, capturedUpdate().getStatus());
        }

        @Test
        @DisplayName("非法：已处于维修中再按接单 → 抛异常且不写库")
        void inProgressCannotBeAcceptedAgain() {
            when(workOrderMapper.selectById(1L)).thenReturn(order(1L, WorkOrderStatus.IN_PROGRESS));

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.accept(1L), "重复接单必须被拒绝");

            assertEquals("只有已派单的工单可以接单", ex.getMessage());
            verify(workOrderMapper, never()).updateById(any(WorkOrder.class));
        }

        @Test
        @DisplayName("非法：已签字/待核价/待报销等其余状态均不可接单（穷举非 ASSIGNED）")
        void otherStatusesCannotBeAccepted() {
            for (WorkOrderStatus status : Arrays.asList(
                    WorkOrderStatus.PENDING_PRICING,
                    WorkOrderStatus.PENDING_SIGN,
                    WorkOrderStatus.PENDING_REIMBURSE,
                    WorkOrderStatus.REIMBURSED)) {
                when(workOrderMapper.selectById(1L)).thenReturn(order(1L, status));
                assertThrows(IllegalArgumentException.class, () -> service.accept(1L),
                        status + " 不应允许接单");
            }
            verify(workOrderMapper, never()).updateById(any(WorkOrder.class));
        }

        @Test
        @DisplayName("异常：工单不存在 → 提示「工单不存在」，不写库")
        void notFound() {
            when(workOrderMapper.selectById(999L)).thenReturn(null);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.accept(999L));

            assertEquals("工单不存在", ex.getMessage());
            verify(workOrderMapper, never()).updateById(any(WorkOrder.class));
        }
    }

    @Nested
    @DisplayName("提交维修 submitRepair()：IN_PROGRESS 出发，按有无耗材分叉")
    class SubmitRepair {

        @Test
        @DisplayName("无耗材：跳过核价，直接进入 PENDING_SIGN，并触发待签字通知")
        void noConsumableSkipsPricing() {
            when(workOrderMapper.selectById(2L)).thenReturn(order(2L, WorkOrderStatus.IN_PROGRESS));

            WorkOrder result = service.submitRepair(2L, "重新配置 DNS 后恢复", Collections.emptyList());

            assertEquals(WorkOrderStatus.PENDING_SIGN, result.getStatus());
            assertEquals(WorkOrderStatus.PENDING_SIGN, capturedUpdate().getStatus());
            assertEquals("重新配置 DNS 后恢复", capturedUpdate().getRepairRecord());
            verify(notificationService, times(1)).notifyPendingSign(any(WorkOrder.class));
            verify(consumableMapper, never()).insert(any(Consumable.class));
        }

        @Test
        @DisplayName("有耗材：进入 PENDING_PRICING，耗材落库数量与字段正确，且不发待签字通知")
        void withConsumableGoesToPricing() {
            when(workOrderMapper.selectById(2L)).thenReturn(order(2L, WorkOrderStatus.IN_PROGRESS));
            ConsumableItem item = new ConsumableItem();
            item.setName("DDR4 8GB");
            item.setQuantity(2);
            item.setIsIou(true);
            item.setIouNo("IOU-001");

            WorkOrder result = service.submitRepair(2L, "更换内存条", Collections.singletonList(item));

            assertEquals(WorkOrderStatus.PENDING_PRICING, result.getStatus());
            assertEquals(WorkOrderStatus.PENDING_PRICING, capturedUpdate().getStatus());

            ArgumentCaptor<Consumable> cap = ArgumentCaptor.forClass(Consumable.class);
            verify(consumableMapper, times(1)).insert(cap.capture());
            Consumable saved = cap.getValue();
            assertEquals(2L, saved.getWorkOrderId());
            assertEquals("DDR4 8GB", saved.getName());
            assertEquals(2, saved.getQuantity());
            assertTrue(saved.getIsIou());
            assertEquals("IOU-001", saved.getIouNo());

            verify(notificationService, never()).notifyPendingSign(any(WorkOrder.class));
        }

        @Test
        @DisplayName("边界：耗材项 name 为空白 → 视为无耗材，仍跳过核价")
        void blankConsumableNameTreatedAsEmpty() {
            when(workOrderMapper.selectById(2L)).thenReturn(order(2L, WorkOrderStatus.IN_PROGRESS));
            ConsumableItem blank = new ConsumableItem();
            blank.setName("   ");
            blank.setQuantity(1);

            WorkOrder result = service.submitRepair(2L, "仅检查配置", Collections.singletonList(blank));

            assertEquals(WorkOrderStatus.PENDING_SIGN, result.getStatus(),
                    "空白耗材名不应把工单拖进核价");
            verify(consumableMapper, never()).insert(any(Consumable.class));
        }

        @Test
        @DisplayName("边界：quantity 为 null 时默认写 1")
        void nullQuantityDefaultsToOne() {
            when(workOrderMapper.selectById(2L)).thenReturn(order(2L, WorkOrderStatus.IN_PROGRESS));
            ConsumableItem noQty = new ConsumableItem();
            noQty.setName("网线");
            noQty.setQuantity(null);

            service.submitRepair(2L, "更换网线", Collections.singletonList(noQty));

            ArgumentCaptor<Consumable> cap = ArgumentCaptor.forClass(Consumable.class);
            verify(consumableMapper).insert(cap.capture());
            assertEquals(1, cap.getValue().getQuantity());
        }

        @Test
        @DisplayName("非法：非维修中状态提交维修 → 抛异常且不写库、不落耗材")
        void cannotSubmitWhenNotInProgress() {
            for (WorkOrderStatus status : Arrays.asList(
                    WorkOrderStatus.ASSIGNED,
                    WorkOrderStatus.PENDING_PRICING,
                    WorkOrderStatus.PENDING_SIGN,
                    WorkOrderStatus.PENDING_REIMBURSE,
                    WorkOrderStatus.REIMBURSED)) {
                when(workOrderMapper.selectById(2L)).thenReturn(order(2L, status));
                assertThrows(IllegalArgumentException.class,
                        () -> service.submitRepair(2L, "x", Collections.emptyList()),
                        status + " 不应允许提交维修");
            }
            verify(workOrderMapper, never()).updateById(any(WorkOrder.class));
            verify(consumableMapper, never()).insert(any(Consumable.class));
        }
    }

    @Nested
    @DisplayName("核价 completePricing()：只允许 PENDING_PRICING → PENDING_SIGN")
    class Pricing {

        @Test
        @DisplayName("合法：待核价 → 待签字，并发出待签字通知")
        void pricingToSign() {
            when(workOrderMapper.selectById(3L)).thenReturn(order(3L, WorkOrderStatus.PENDING_PRICING));

            WorkOrder result = service.completePricing(3L);

            assertEquals(WorkOrderStatus.PENDING_SIGN, result.getStatus());
            assertEquals(WorkOrderStatus.PENDING_SIGN, capturedUpdate().getStatus());
            verify(notificationService).notifyPendingSign(any(WorkOrder.class));
        }

        @Test
        @DisplayName("非法：非待核价状态核价 → 抛异常且不写库")
        void cannotPriceOtherStatus() {
            for (WorkOrderStatus status : Arrays.asList(
                    WorkOrderStatus.ASSIGNED,
                    WorkOrderStatus.IN_PROGRESS,
                    WorkOrderStatus.PENDING_SIGN,
                    WorkOrderStatus.PENDING_REIMBURSE,
                    WorkOrderStatus.REIMBURSED)) {
                when(workOrderMapper.selectById(3L)).thenReturn(order(3L, status));
                assertThrows(IllegalArgumentException.class, () -> service.completePricing(3L),
                        status + " 不应允许核价");
            }
            verify(workOrderMapper, never()).updateById(any(WorkOrder.class));
        }
    }

    @Nested
    @DisplayName("签字 sign()：只允许 PENDING_SIGN → PENDING_REIMBURSE")
    class Sign {

        @Test
        @DisplayName("合法：签字后状态推进为待报销，审计字段齐全")
        void signAdvancesToReimburse() {
            when(workOrderMapper.selectById(4L)).thenReturn(order(4L, WorkOrderStatus.PENDING_SIGN));

            WorkOrder result = service.sign(4L, "护士长王五", false, null, null, "TYPED");

            assertEquals(WorkOrderStatus.PENDING_REIMBURSE, result.getStatus());
            WorkOrder saved = capturedUpdate();
            assertEquals(WorkOrderStatus.PENDING_REIMBURSE, saved.getStatus());
            assertEquals("护士长王五", saved.getSignedBy());
            assertEquals("TYPED", saved.getSignType().name());
            assertEquals(com.itservice.desk.model.SignStatus.SIGNED, saved.getSignStatus());
            assertTrue(saved.getSignedAt() != null, "签字时间必须落库，否则审计断链");
            assertTrue(saved.getCompletedAt() != null);
        }

        @Test
        @DisplayName("代签：签名人需标注「由 X 代签」，便于追责")
        void delegateSignatureIsMarked() {
            when(workOrderMapper.selectById(4L)).thenReturn(order(4L, WorkOrderStatus.PENDING_SIGN));

            service.sign(4L, "护士长王五", true, "责任护士赵六", null, "HANDWRITTEN");

            assertEquals("责任护士赵六(由护士长王五代签)", capturedUpdate().getSignedBy());
        }

        @Test
        @DisplayName("脏值容错：非法 signType 回落为 HANDWRITTEN，不污染审计字段")
        void invalidSignTypeFallsBack() {
            when(workOrderMapper.selectById(4L)).thenReturn(order(4L, WorkOrderStatus.PENDING_SIGN));

            service.sign(4L, "护士长王五", false, null, null, "SCRIBBLE");

            assertEquals("HANDWRITTEN", capturedUpdate().getSignType().name());
        }

        @Test
        @DisplayName("非法：非待签字状态签字 → 抛异常且不写库（防重复签字）")
        void cannotSignOtherStatus() {
            for (WorkOrderStatus status : Arrays.asList(
                    WorkOrderStatus.ASSIGNED,
                    WorkOrderStatus.IN_PROGRESS,
                    WorkOrderStatus.PENDING_PRICING,
                    WorkOrderStatus.PENDING_REIMBURSE,
                    WorkOrderStatus.REIMBURSED)) {
                when(workOrderMapper.selectById(4L)).thenReturn(order(4L, status));
                assertThrows(IllegalArgumentException.class,
                        () -> service.sign(4L, "护士长王五", false, null, null, "TYPED"),
                        status + " 不应允许签字");
            }
            verify(workOrderMapper, never()).updateById(any(WorkOrder.class));
        }
    }

    @Nested
    @DisplayName("报销 reimburse()：只允许 PENDING_REIMBURSE → REIMBURSED")
    class Reimburse {

        @Test
        @DisplayName("合法：待报销 → 已报销")
        void pendingReimburseToReimbursed() {
            when(workOrderMapper.selectById(5L)).thenReturn(order(5L, WorkOrderStatus.PENDING_REIMBURSE));

            WorkOrder result = service.reimburse(5L);

            assertEquals(WorkOrderStatus.REIMBURSED, result.getStatus());
            assertEquals(WorkOrderStatus.REIMBURSED, capturedUpdate().getStatus());
        }

        @Test
        @DisplayName("非法：非待报销状态报销 → 抛异常且不写库")
        void cannotReimburseOtherStatus() {
            for (WorkOrderStatus status : Arrays.asList(
                    WorkOrderStatus.ASSIGNED,
                    WorkOrderStatus.IN_PROGRESS,
                    WorkOrderStatus.PENDING_PRICING,
                    WorkOrderStatus.PENDING_SIGN,
                    WorkOrderStatus.REIMBURSED)) {
                when(workOrderMapper.selectById(5L)).thenReturn(order(5L, status));
                assertThrows(IllegalArgumentException.class, () -> service.reimburse(5L),
                        status + " 不应允许报销");
            }
            verify(workOrderMapper, never()).updateById(any(WorkOrder.class));
        }
    }

    @Nested
    @DisplayName("全局不变量：状态机只允许 5 条正向迁移，其余 25 种组合一律拒绝")
    class FullTransitionMatrix {

        /**
         * 6 个状态 × 5 个动作方法 = 30 种组合；
         * 合法组合仅 5 条：ASSIGNED→accept、IN_PROGRESS→repair、PENDING_PRICING→pricing、
         * PENDING_SIGN→sign、PENDING_REIMBURSE→reimburse。
         */
        @Test
        @DisplayName("穷举 30 种「状态×动作」组合，仅 5 种合法，25 种必须被拒绝")
        void onlyFiveTransitionsAreLegal() {
            int total = 0;
            int legal = 0;
            int rejected = 0;
            java.util.List<String> legalCases = new java.util.ArrayList<>();
            java.util.List<String> rejectedCases = new java.util.ArrayList<>();

            for (WorkOrderStatus status : WorkOrderStatus.values()) {
                java.util.Map<String, Runnable> actions = new java.util.LinkedHashMap<>();
                actions.put("accept", () -> service.accept(1L));
                actions.put("repair", () -> service.submitRepair(1L, "记录", Collections.emptyList()));
                actions.put("pricing", () -> service.completePricing(1L));
                actions.put("sign", () -> service.sign(1L, "签字人", false, null, null, "TYPED"));
                actions.put("reimburse", () -> service.reimburse(1L));

                for (java.util.Map.Entry<String, Runnable> e : actions.entrySet()) {
                    total++;
                    String label = status.name() + " + " + e.getKey();
                    if (expectOrReject(status, e.getValue())) {
                        legal++;
                        legalCases.add(label);
                    } else {
                        rejected++;
                        rejectedCases.add(label);
                    }
                }
            }

            assertEquals(30, total, "必须穷举 6 状态 × 5 动作 = 30 组");
            assertEquals(5, legal, "合法迁移应恰好 5 条，实际合法：" + legalCases);
            assertEquals(25, rejected, "其余 25 种组合必须被拒绝，实际被放行：" + rejectedCases);
            assertEquals(java.util.Arrays.asList(
                            "ASSIGNED + accept",
                            "IN_PROGRESS + repair",
                            "PENDING_PRICING + pricing",
                            "PENDING_SIGN + sign",
                            "PENDING_REIMBURSE + reimburse"),
                    legalCases, "合法迁移集合必须与设计完全一致");
        }

        /** @return true 表示该动作被放行（合法迁移），false 表示被拒绝 */
        private boolean expectOrReject(WorkOrderStatus status, Runnable call) {
            // 每次都要全新的工单实例：Service 会原地修改传入实体，
            // 复用同一实例会让后续状态判断错乱（本测试第一版即因此失败）。
            when(workOrderMapper.selectById(anyLong())).thenReturn(order(1L, status));
            try {
                call.run();
                return true;
            } catch (IllegalArgumentException e) {
                return false;
            }
        }
    }
}

