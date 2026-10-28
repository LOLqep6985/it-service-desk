package com.itservice.desk.controller;

import com.itservice.desk.dto.ApiResponse;
import com.itservice.desk.mapper.ConsumableMapper;
import com.itservice.desk.mapper.DepartmentBindingMapper;
import com.itservice.desk.mapper.UserMapper;
import com.itservice.desk.model.User;
import com.itservice.desk.model.UserRole;
import com.itservice.desk.model.WorkOrder;
import com.itservice.desk.model.WorkOrderStatus;
import com.itservice.desk.service.WorkOrderAccess;
import com.itservice.desk.service.WorkOrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 回归测试：锁定 {@code POST /api/work-orders/{id}/accept} 的
 * <b>第一道守卫（Controller 层 403）</b>，与
 * {@link com.itservice.desk.service.AcceptStateGuardRegressionTest}（第二道守卫，Service 层异常）
 * 组成"纵深防御"双保险。
 *
 * <h2>为什么这一层也要回归</h2>
 * 本轮变异（删除 Service 的 status 守卫）在 HTTP 层<b>不会暴露</b>，因为 Controller 先返回 403。
 * 但反过来说：<b>如果有人误删 Controller 这段判断</b>，请求就会穿透到 Service，
 * 此时唯一的防线是 Service 的异常——异常若被 catch 掉或未捕获，就会向客户端返回 500 甚至假成功。
 * 因此两层各需要一个回归锚点，任一层被削弱都能被单独发现。
 *
 * <h2>变异前正确行为（本测试预期值）</h2>
 * <ol>
 *   <li>无 token / token 非法 → {@code code=401}；</li>
 *   <li>非被指派的维修工（含管理员、其它维修工、财务）→ {@code code=403}；</li>
 *   <li>被指派维修工但工单非 ASSIGNED → {@code code=403}；</li>
 *   <li>被拒绝时<b>绝不调用</b> {@code workOrderService.accept(...)}（不触碰写路径）。</li>
 * </ol>
 *
 * <p><b>说明</b>：本测试直接实例化 Controller 并注入 Mock，属于纯单元测试，
 * 不启动 Spring 容器，也不依赖 MySQL。这样无需新增 H2/Testcontainers 依赖
 * （离线仓库中二者均缺失），可稳定在 CI 与本地复跑。
 */
class WorkOrderControllerAcceptRegressionTest {

    private WorkOrderService workOrderService;
    private UserMapper userMapper;
    private WorkOrderAccess access;
    private com.itservice.desk.security.TokenService tokenService;
    private com.itservice.desk.security.DeptTokenService deptTokenService;
    private com.itservice.desk.security.AuthUserResolver authUserResolver;
    private WorkOrderController controller;

    @BeforeEach
    void setUp() {
        workOrderService = mock(WorkOrderService.class);
        userMapper = mock(UserMapper.class);
        // 权限判定用真实实现（纯规则类，无外部依赖），避免"用 Mock 测 Mock"的空转
        access = new WorkOrderAccess();
        tokenService = new com.itservice.desk.security.TokenService("test-secret", 12);
        deptTokenService = new com.itservice.desk.security.DeptTokenService("test-dept-secret", 720);
        authUserResolver = new com.itservice.desk.security.AuthUserResolver(tokenService, userMapper);
        controller = new WorkOrderController(
                workOrderService,
                access,
                userMapper,
                mock(DepartmentBindingMapper.class),
                mock(ConsumableMapper.class),
                authUserResolver,
                deptTokenService);
    }

    private static User user(long id, UserRole role) {
        User u = new User();
        u.setId(id);
        u.setRole(role);
        u.setName("U" + id);
        return u;
    }

    private static WorkOrder order(Long id, WorkOrderStatus status, Long assigneeId) {
        WorkOrder wo = new WorkOrder();
        wo.setId(id);
        wo.setStatus(status);
        wo.setAssigneeId(assigneeId);
        wo.setDepartmentId(6L);
        wo.setTitle("打印机报修");
        return wo;
    }

    @Nested
    @DisplayName("R6 第一道守卫：凭证与身份")
    class AuthGuard {

        @Test
        @DisplayName("R6-a 无 token → 401，且不调用业务方法")
        void noTokenGives401() {
            ApiResponse res = controller.accept(1L, null);

            assertEquals(401, res.getCode());
            assertEquals("未登录或登录已过期", res.getMessage());
            verify(workOrderService, never()).accept(anyLong());
        }

        @Test
        @DisplayName("R6-b 伪造 token（token-999 无对应用户）→ 401")
        void forgedTokenGives401() {
            when(userMapper.selectById(999L)).thenReturn(null);

            ApiResponse res = controller.accept(1L, "token-999");

            assertEquals(401, res.getCode());
            verify(workOrderService, never()).accept(anyLong());
        }

        @Test
        @DisplayName("R6-c 非 token- 前缀的任意字符串 → 401，不抛异常")
        void garbageTokenGives401() {
            ApiResponse res = controller.accept(1L, "Bearer abc.def.ghi");

            assertEquals(401, res.getCode());
            verify(workOrderService, never()).accept(anyLong());
        }
    }

    @Nested
    @DisplayName("R7 第一道守卫：角色与指派关系")
    class RoleGuard {

        @Test
        @DisplayName("R7-a 管理员也不能「接单」（接单是维修工专属动作）→ 403")
        void adminCannotAccept() {
            when(userMapper.selectById(1L)).thenReturn(user(1L, UserRole.ADMIN));
            when(workOrderService.getById(1L)).thenReturn(order(1L, WorkOrderStatus.ASSIGNED, 3L));

            ApiResponse res = controller.accept(1L, tokenService.issue(1L));

            assertEquals(403, res.getCode(), "管理员可看全部工单，但接单是维修工的现场动作");
            verify(workOrderService, never()).accept(anyLong());
        }

        @Test
        @DisplayName("R7-b 财务 → 403（职责分离：财务不接触工单）")
        void financeCannotAccept() {
            when(userMapper.selectById(6L)).thenReturn(user(6L, UserRole.FINANCE));
            when(workOrderService.getById(1L)).thenReturn(order(1L, WorkOrderStatus.ASSIGNED, 3L));

            assertEquals(403, controller.accept(1L, tokenService.issue(6L)).getCode());
            verify(workOrderService, never()).accept(anyLong());
        }

        @Test
        @DisplayName("R7-c 其它维修工抢单 → 403（横向越权）")
        void otherRepairerCannotAccept() {
            when(userMapper.selectById(4L)).thenReturn(user(4L, UserRole.REPAIRER));
            when(workOrderService.getById(1L)).thenReturn(order(1L, WorkOrderStatus.ASSIGNED, 3L));

            ApiResponse res = controller.accept(1L, tokenService.issue(4L));

            assertEquals(403, res.getCode());
            verify(workOrderService, never()).accept(anyLong());
        }
    }

    @Nested
    @DisplayName("R8 第一道守卫：状态前置（与 Service 守卫同源）")
    class StatusGuardAtController {

        @Test
        @DisplayName("R8-a 被指派维修工 + 工单已报销 → 403，请求不穿透到 Service")
        void assignedRepairerButReimbursedGives403() {
            when(userMapper.selectById(3L)).thenReturn(user(3L, UserRole.REPAIRER));
            when(workOrderService.getById(1L)).thenReturn(order(1L, WorkOrderStatus.REIMBURSED, 3L));

            ApiResponse res = controller.accept(1L, tokenService.issue(3L));

            assertEquals(403, res.getCode());
            assertEquals("无权限接单：只有被指派的维修人员在「待接单」状态可以接单", res.getMessage());
            // 关键断言：非 500、非成功，且完全没有进入写路径
            verify(workOrderService, never()).accept(anyLong());
        }

        @Test
        @DisplayName("R8-b 被指派维修工 + 待接单 → 放行，且确实调用一次 Service")
        void assignedRepairerAndAssignedStatusPasses() {
            when(userMapper.selectById(3L)).thenReturn(user(3L, UserRole.REPAIRER));
            WorkOrder wo = order(1L, WorkOrderStatus.ASSIGNED, 3L);
            when(workOrderService.getById(1L)).thenReturn(wo);
            when(workOrderService.accept(1L)).thenReturn(wo);

            ApiResponse res = controller.accept(1L, tokenService.issue(3L));

            assertEquals(0, res.getCode());
            verify(workOrderService).accept(1L);
        }
    }

    @Nested
    @DisplayName("R9 第二道守卫仍生效：Controller 捕获 Service 异常并转业务错误码")
    class ServiceExceptionPropagation {

        @Test
        @DisplayName("R9 Service 抛 IllegalArgumentException 时，Controller 转成 code=500 业务提示而非裸异常")
        void serviceExceptionBecomesBusinessError() {
            when(userMapper.selectById(3L)).thenReturn(user(3L, UserRole.REPAIRER));
            when(workOrderService.getById(1L)).thenReturn(order(1L, WorkOrderStatus.ASSIGNED, 3L));
            // 模拟并发竞态：权限判断时是 ASSIGNED，真正执行时已被别人改成 IN_PROGRESS
            when(workOrderService.accept(1L))
                    .thenThrow(new IllegalArgumentException("只有已派单的工单可以接单"));

            ApiResponse res = controller.accept(1L, tokenService.issue(3L));

            assertEquals(500, res.getCode(),
                    "业务异常必须被转成统一返回体，而不是抛到全局变成裸 500 页面");
            assertEquals("只有已派单的工单可以接单", res.getMessage());
        }
    }
}
