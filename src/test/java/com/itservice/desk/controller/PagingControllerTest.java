package com.itservice.desk.controller;

import com.itservice.desk.dto.ApiResponse;
import com.itservice.desk.dto.PageResult;
import com.itservice.desk.mapper.ConsumableMapper;
import com.itservice.desk.mapper.DepartmentBindingMapper;
import com.itservice.desk.mapper.UserMapper;
import com.itservice.desk.model.User;
import com.itservice.desk.model.UserRole;
import com.itservice.desk.model.WorkOrder;
import com.itservice.desk.model.WorkOrderStatus;
import com.itservice.desk.security.AuthUserResolver;
import com.itservice.desk.security.DeptTokenService;
import com.itservice.desk.security.TokenService;
import com.itservice.desk.service.WorkOrderAccess;
import com.itservice.desk.service.WorkOrderService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 列表分页的 Controller 层回归测试（待办三扩展）。
 *
 * <h2>为什么这层必须测</h2>
 * 分页改动最大的风险不是"分页算错"，而是<b>破坏老前端契约</b>：
 * 前端多处把返回值直接当数组用（{@code state.rows.filter(...)}）。
 * 若把不传 page 的请求也改成分页对象，页面会立刻报 {@code filter is not a function}。
 *
 * <h2>核心不变量</h2>
 * <ol>
 *   <li><b>不传 page → 仍返回 List</b>（向后兼容，本次最重要的一条）；</li>
 *   <li><b>传 page → 返回 PageResult</b>，含 records/total/page/size/pages；</li>
 *   <li>page 小于 1 时按下限 1 处理，不能抛异常；</li>
 *   <li>size 无上限时被夹到 200，防止 {@code size=999999} 变相拉全量；</li>
 *   <li>未认证仍然 401 —— 分页不能绕过鉴权。</li>
 * </ol>
 */
class PagingControllerTest {

    private WorkOrderService workOrderService;
    private UserMapper userMapper;
    private TokenService tokenService;
    private WorkOrderController controller;

    @BeforeEach
    void setUp() {
        workOrderService = mock(WorkOrderService.class);
        userMapper = mock(UserMapper.class);
        tokenService = new TokenService("paging-test-secret", 12);
        DeptTokenService deptTokenService = new DeptTokenService("paging-dept-secret", 720);
        controller = new WorkOrderController(
                workOrderService,
                new WorkOrderAccess(),
                userMapper,
                mock(DepartmentBindingMapper.class),
                mock(ConsumableMapper.class),
                new AuthUserResolver(tokenService, userMapper),
                deptTokenService);
    }

    private String adminToken() {
        return tokenService.issue(1L);
    }

    private void stubAdmin() {
        User admin = new User();
        admin.setId(1L);
        admin.setRole(UserRole.ADMIN);
        admin.setName("系统管理员");
        when(userMapper.selectById(1L)).thenReturn(admin);
    }

    private static Page<WorkOrder> page(long current, long size, long total, int records) {
        Page<WorkOrder> p = new Page<>(current, size);
        java.util.List<WorkOrder> list = new java.util.ArrayList<>();
        for (int i = 0; i < records; i++) {
            WorkOrder wo = new WorkOrder();
            wo.setId((long) i + 1);
            wo.setStatus(WorkOrderStatus.ASSIGNED);
            wo.setTitle("报修" + i);
            list.add(wo);
        }
        p.setRecords(list);
        p.setTotal(total);
        return p;
    }

    @Nested
    @DisplayName("向后兼容：不传 page 必须仍返回数组")
    class BackwardCompatibility {

        @Test
        @DisplayName("核心：不传 page → data 是 List，不是 PageResult")
        void withoutPageReturnsPlainList() {
            stubAdmin();
            when(workOrderService.listAll(null))
                    .thenReturn(Collections.singletonList(new WorkOrder()));

            ApiResponse res = controller.list(adminToken(), null, null, null, null);

            assertEquals(0, res.getCode());
            assertInstanceOf(java.util.List.class, res.getData(),
                    "老前端把 data 当数组用（.filter/.map），这里绝不能变成对象");
            assertTrue(res.getData() instanceof java.util.List);
            // 关键：走的是老方法，不是分页方法
            verify(workOrderService).listAll(null);
            verify(workOrderService, never()).pageAll(any(), any(), anyLong(), anyLong());
        }

        @Test
        @DisplayName("不传 page 时 keyword 参数被忽略（老接口无搜索能力，行为不变）")
        void keywordIgnoredWithoutPage() {
            stubAdmin();
            when(workOrderService.listAll("ASSIGNED"))
                    .thenReturn(Collections.emptyList());

            ApiResponse res = controller.list(adminToken(), "ASSIGNED", null, null, "任意词");

            assertEquals(0, res.getCode());
            assertInstanceOf(java.util.List.class, res.getData());
        }
    }

    @Nested
    @DisplayName("分页模式：传 page 返回 PageResult")
    class ServerPaging {

        @Test
        @DisplayName("传 page → 返回 PageResult 且字段完整")
        void withPageReturnsPageResult() {
            stubAdmin();
            when(workOrderService.pageAll(isNull(), isNull(), eq(1L), eq(10L)))
                    .thenReturn(page(1, 10, 30, 10));

            ApiResponse res = controller.list(adminToken(), null, 1, 10, null);

            assertEquals(0, res.getCode());
            assertInstanceOf(PageResult.class, res.getData());
            @SuppressWarnings("unchecked")
            PageResult<WorkOrder> pr = (PageResult<WorkOrder>) res.getData();
            assertEquals(10, pr.getRecords().size());
            assertEquals(30, pr.getTotal());
            assertEquals(1, pr.getPage());
            assertEquals(10, pr.getSize());
            assertEquals(3, pr.getPages(), "30 条 / 每页 10 → 3 页");
        }

        @Test
        @DisplayName("page < 1 时按下限 1 处理，不报错")
        void pageBelowOneIsClamped() {
            stubAdmin();
            when(workOrderService.pageAll(isNull(), isNull(), eq(1L), eq(10L)))
                    .thenReturn(page(1, 10, 5, 5));

            ApiResponse res = controller.list(adminToken(), null, 0, 10, null);

            assertEquals(0, res.getCode(), "page=0 不应报错，应夹到 1");
            verify(workOrderService).pageAll(isNull(), isNull(), eq(1L), eq(10L));
        }

        @Test
        @DisplayName("size 超上限 → 夹到 200，防止变相全量")
        void oversizedSizeIsClamped() {
            stubAdmin();
            when(workOrderService.pageAll(isNull(), isNull(), eq(1L), eq(200L)))
                    .thenReturn(page(1, 200, 30, 30));

            ApiResponse res = controller.list(adminToken(), null, 1, 999999, null);

            assertEquals(0, res.getCode());
            verify(workOrderService).pageAll(isNull(), isNull(), eq(1L), eq(200L));
        }

        @Test
        @DisplayName("size 为 null → 默认 20")
        void nullSizeDefaultsTo20() {
            stubAdmin();
            when(workOrderService.pageAll(isNull(), isNull(), eq(1L), eq(20L)))
                    .thenReturn(page(1, 20, 0, 0));

            controller.list(adminToken(), null, 1, null, null);

            verify(workOrderService).pageAll(isNull(), isNull(), eq(1L), eq(20L));
        }

        @Test
        @DisplayName("keyword 透传到 Service（服务端搜索，避免只搜当前页）")
        void keywordPassedThrough() {
            stubAdmin();
            when(workOrderService.pageAll(isNull(), eq("打印机"), eq(1L), eq(10L)))
                    .thenReturn(page(1, 10, 3, 3));

            ApiResponse res = controller.list(adminToken(), null, 1, 10, "打印机");

            assertEquals(0, res.getCode());
            verify(workOrderService).pageAll(isNull(), eq("打印机"), eq(1L), eq(10L));
        }

        @Test
        @DisplayName("status 与分页组合生效")
        void statusCombinesWithPaging() {
            stubAdmin();
            when(workOrderService.pageAll(eq("ASSIGNED"), isNull(), eq(1L), eq(10L)))
                    .thenReturn(page(1, 10, 8, 8));

            controller.list(adminToken(), "ASSIGNED", 1, 10, null);

            verify(workOrderService).pageAll(eq("ASSIGNED"), isNull(), eq(1L), eq(10L));
        }
    }

    @Nested
    @DisplayName("鉴权不能被分页绕过")
    class AuthStillEnforced {

        @Test
        @DisplayName("无 token + 分页参数 → 仍必须 401")
        void noTokenStill401() {
            ApiResponse res = controller.list(null, null, 1, 10, null);

            assertEquals(401, res.getCode(), "带分页参数不能绕过登录校验");
            verify(workOrderService, never()).pageAll(any(), any(), anyLong(), anyLong());
            verify(workOrderService, never()).listAll(any());
        }

        @Test
        @DisplayName("维修工 + 分页参数 → 仍必须 403")
        void wrongRoleStill403() {
            User repairer = new User();
            repairer.setId(3L);
            repairer.setRole(UserRole.REPAIRER);
            repairer.setName("张工");
            when(userMapper.selectById(3L)).thenReturn(repairer);

            ApiResponse res = controller.list(tokenService.issue(3L), null, 1, 10, null);

            assertEquals(403, res.getCode(), "分页不能绕过角色校验");
            verify(workOrderService, never()).pageAll(any(), any(), anyLong(), anyLong());
        }
    }
}
