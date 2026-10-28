package com.itservice.desk.service;

import com.itservice.desk.dto.PageResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 事务边界与分页响应体的回归测试（待办二 / 待办三）。
 *
 * <h2>待办二背景</h2>
 * 修复前全项目仅 1 处 {@code @Transactional}（{@code ReimbursementService:374}），
 * 而以下方法都会写多张表，任一失败都会留下脏数据：
 * <ul>
 *   <li>{@code WorkOrderService.sign}：改 work_order + 改 report</li>
 *   <li>{@code WorkOrderService.submitRepair}：插 consumable + 改 work_order</li>
 *   <li>{@code WorkOrderService.reimburse}：改 work_order + 改 report</li>
 *   <li>{@code WorkOrderService.completePricing}：改 work_order + 发通知</li>
 *   <li>{@code ReportService.dispatch}：插 work_order + 改 report</li>
 * </ul>
 *
 * <h2>为什么用反射测试</h2>
 * 真实回滚需要数据库与故障注入，属集成测试范围（见交付文档"待补"）。
 * 本层先保证"注解确实存在且 rollbackFor 正确"——
 * 少了 {@code rollbackFor = Exception.class} 时 Spring 只对 RuntimeException 回滚，
 * 受检异常不会回滚，这是极易被忽略的坑。
 */
class TransactionAndPagingTest {

    /** 必须被事务包裹的多表写入方法 */
    private static final List<String[]> MULTI_TABLE_METHODS = Arrays.asList(
            new String[]{"com.itservice.desk.service.WorkOrderService", "sign"},
            new String[]{"com.itservice.desk.service.WorkOrderService", "submitRepair"},
            new String[]{"com.itservice.desk.service.WorkOrderService", "reimburse"},
            new String[]{"com.itservice.desk.service.WorkOrderService", "completePricing"},
            new String[]{"com.itservice.desk.service.ReportService", "dispatch"}
    );

    @Test
    @DisplayName("待办二：所有多表写入方法都必须标注 @Transactional")
    void allMultiTableWritesAreTransactional() {
        for (String[] target : MULTI_TABLE_METHODS) {
            Transactional tx = findAnnotation(target[0], target[1]);
            assertNotNull(tx,
                    target[0].substring(target[0].lastIndexOf('.') + 1) + "." + target[1]
                            + " 涉及多表写入，必须有 @Transactional，否则中途失败会留下脏数据");
        }
    }

    @Test
    @DisplayName("待办二：rollbackFor 必须包含 Exception，不能只回滚 RuntimeException")
    void rollbackForCoversCheckedExceptions() {
        for (String[] target : MULTI_TABLE_METHODS) {
            Transactional tx = findAnnotation(target[0], target[1]);
            assertNotNull(tx);
            boolean coversException = Arrays.stream(tx.rollbackFor())
                    .anyMatch(c -> c == Exception.class || c == Throwable.class);
            assertTrue(coversException,
                    target[1] + " 的 rollbackFor 应包含 Exception；"
                            + "默认只回滚 RuntimeException，受检异常会导致部分提交");
        }
    }

    @Test
    @DisplayName("待办三：PageResult 能正确承载分页元数据")
    void pageResultCarriesMetadata() {
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<String> page =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(2, 10);
        page.setRecords(Arrays.asList("a", "b", "c"));
        page.setTotal(25);

        PageResult<String> result = new PageResult<>(page);

        assertEquals(Arrays.asList("a", "b", "c"), result.getRecords());
        assertEquals(25, result.getTotal());
        assertEquals(2, result.getPage());
        assertEquals(10, result.getSize());
        assertEquals(3, result.getPages(), "25 条 / 每页 10 → 3 页");
    }

    @Test
    @DisplayName("待办三：PageResult 空页也能安全序列化（无 NPE）")
    void emptyPageIsSafe() {
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<String> page =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(1, 20);
        page.setRecords(java.util.Collections.emptyList());
        page.setTotal(0);

        PageResult<String> result = new PageResult<>(page);

        assertNotNull(result.getRecords());
        assertTrue(result.getRecords().isEmpty());
        assertEquals(0, result.getTotal());
        assertEquals(0, result.getPages(), "0 条时总页数应为 0，前端据此隐藏翻页器");
    }

    private static Transactional findAnnotation(String className, String methodName) {
        try {
            Class<?> clazz = Class.forName(className);
            for (Method m : clazz.getDeclaredMethods()) {
                if (m.getName().equals(methodName) && m.getAnnotation(Transactional.class) != null) {
                    return m.getAnnotation(Transactional.class);
                }
            }
            return null;
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("类不存在: " + className, e);
        }
    }
}
