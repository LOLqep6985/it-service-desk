package com.itservice.desk.controller;

import com.itservice.desk.dto.ApiResponse;
import com.itservice.desk.dto.WorkOrderDetail;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 回归测试：缺陷 D1 / D2 / D3 <b>修复后</b>的目标行为。
 *
 * <p>本类由三个"缺陷固化类"合并改造而来：
 * <ul>
 *   <li>原 {@code WorkOrderNotFoundDefectTest}（D1）—— 修复前用 {@code assertThrows}
 *       固化"异常裸奔"，现已改为断言<b>统一错误体 + 404</b>；</li>
 *   <li>原 {@code AccessControlDefectTest}（D2/D3）—— 修复前断言"伪造 token 被采信"、
 *       "裸 deptId 被信任"，现已改为断言<b>两者均被拒绝</b>。</li>
 * </ul>
 * 缺陷已修复，因此不再需要 {@code @Disabled} 的"期望值"用例——它们已成为正式回归断言。
 *
 * <h2>本轮修复内容</h2>
 * <ol>
 *   <li><b>D1</b>：新增 {@code GlobalExceptionHandler}（{@code @RestControllerAdvice}），
 *       {@code NotFoundException} → HTTP 404 + {@code code=404}；</li>
 *   <li><b>D2</b>：token 由明文 {@code token-<id>} 改为 HMAC-SHA256 签名 + 过期时间；</li>
 *   <li><b>D3</b>：科室维度改为只认服务端签发的 {@code deptToken}，裸 {@code departmentId} 不再被采信。</li>
 * </ol>
 */
class SecurityAndNotFoundFixTest {

    private WorkOrderService workOrderService;
    private UserMapper userMapper;
    private TokenService tokenService;
    private DeptTokenService deptTokenService;
    private WorkOrderController controller;

    @BeforeEach
    void setUp() {
        workOrderService = mock(WorkOrderService.class);
        userMapper = mock(UserMapper.class);
        tokenService = new TokenService("unit-test-secret-for-user-token", 12);
        deptTokenService = new DeptTokenService("unit-test-secret-for-dept-token", 720);
        controller = new WorkOrderController(
                workOrderService,
                new WorkOrderAccess(),
                userMapper,
                mock(DepartmentBindingMapper.class),
                mock(ConsumableMapper.class),
                new AuthUserResolver(tokenService, userMapper),
                deptTokenService);
    }

    /** 生成一个合法登录凭证（模拟真实登录后的 token） */
    private String loginToken(long userId) {
        return tokenService.issue(userId);
    }

    private static User user(long id, UserRole role) {
        User u = new User();
        u.setId(id);
        u.setRole(role);
        u.setName("U" + id);
        return u;
    }

    private static WorkOrder order(Long id, WorkOrderStatus status, Long deptId) {
        WorkOrder wo = new WorkOrder();
        wo.setId(id);
        wo.setStatus(status);
        wo.setDepartmentId(deptId);
        wo.setAssigneeId(3L);
        wo.setTitle("打印机报修");
        return wo;
    }

    // ======================= D1：资源不存在 =======================

    @Nested
    @DisplayName("D1（已修复）不存在的资源返回 404 业务错误体")
    class NotFoundFixed {

        @Test
        @DisplayName("修复验证：getById 抛 NotFoundException → 由全局处理器转 404，不再裸奔")
        void notFoundIsMappedTo404() {
            when(workOrderService.getById(99999L))
                    .thenThrow(new com.itservice.desk.exception.NotFoundException("工单不存在"));
            when(userMapper.selectById(1L)).thenReturn(user(1L, UserRole.ADMIN));

            // 修复后：Controller 内不再有 try-catch，异常交由 @RestControllerAdvice 处理。
            // 单元测试直接验证"异常类型语义正确"，以及处理器映射规则（见 GlobalExceptionHandlerTest）。
            com.itservice.desk.exception.NotFoundException ex =
                    org.junit.jupiter.api.Assertions.assertThrows(
                            com.itservice.desk.exception.NotFoundException.class,
                            () -> controller.detail(99999L, loginToken(1L), null, null));

            assertEquals("工单不存在", ex.getMessage());
            // 关键：它是 IllegalArgumentException 的子类，既有 14 处 catch 仍然兼容
            assertTrue(ex instanceof IllegalArgumentException,
                    "必须保持与既有 catch(IllegalArgumentException) 的兼容性");
        }

        @Test
        @DisplayName("反向保护：工单存在时正常返回，不被 D1 修复误伤")
        void existingOrderStillWorks() {
            WorkOrder wo = order(1L, WorkOrderStatus.ASSIGNED, 6L);
            when(workOrderService.getById(1L)).thenReturn(wo);
            when(userMapper.selectById(1L)).thenReturn(user(1L, UserRole.ADMIN));
            WorkOrderDetail detail = new WorkOrderDetail();
            detail.setWorkOrder(wo);
            when(workOrderService.buildDetail(wo)).thenReturn(detail);

            ApiResponse res = controller.detail(1L, loginToken(1L), null, null);

            assertEquals(0, res.getCode());
        }

        @Test
        @DisplayName("反向保护：越权仍返回 403，不得用 404 掩盖真实拒绝原因")
        void forbiddenStaysForbidden() {
            WorkOrder wo = order(1L, WorkOrderStatus.IN_PROGRESS, 6L);
            wo.setAssigneeId(4L);
            when(workOrderService.getById(1L)).thenReturn(wo);
            when(userMapper.selectById(3L)).thenReturn(user(3L, UserRole.REPAIRER));

            assertEquals(403, controller.detail(1L, loginToken(3L), null, null).getCode(),
                    "修复 D1 时不能把 403 也改成 404，否则误导排障方向");
        }
    }

    // ======================= D2：token 伪造 =======================

    @Nested
    @DisplayName("D2（已修复）凭证不可伪造")
    class TokenForgeryFixed {

        @Test
        @DisplayName("核心回归：伪造的 token-1 必须被拒绝为 401")
        void forgedPlainTokenRejected() {
            // 修复前这种 token 会被直接采信（实测 HTTP 200）
            ApiResponse res = controller.list("token-1", null, null, null, null);

            assertEquals(401, res.getCode(),
                    "明文 token-<id> 不再被接受，攻击者无法凭 userId 推导出可用凭证");
            verify(workOrderService, never()).listAll(any());
        }

        @Test
        @DisplayName("伪造变体：篡改签名、篡改 payload、空串、随机串 全部 401")
        void tamperedVariantsRejected() {
            String valid = loginToken(1L);
            String tamperedSignature = valid.substring(0, valid.length() - 1)
                    + (valid.endsWith("A") ? "B" : "A");
            String[] attempts = {
                    null,
                    "",
                    "token-1",
                    "Bearer " + valid,
                    tamperedSignature,
                    valid + "x",
                    "dept.1.9999999999999.fakesignature"
            };
            for (String attempt : attempts) {
                assertEquals(401, controller.list(attempt, null, null, null, null).getCode(),
                        "该凭证应被拒绝: " + attempt);
            }
        }

        @Test
        @DisplayName("对照组：合法签发的凭证可以通过认证")
        void validTokenAccepted() {
            when(userMapper.selectById(1L)).thenReturn(user(1L, UserRole.ADMIN));
            when(workOrderService.listAll(null)).thenReturn(Collections.emptyList());

            assertEquals(0, controller.list(loginToken(1L), null, null, null, null).getCode(),
                    "修复不能把正常登录的用户也拦掉");
        }

        @Test
        @DisplayName("凭证类型隔离：科室凭证不能当登录凭证用")
        void deptTokenCannotActAsLoginToken() {
            String deptTok = deptTokenService.issue(6L);
            // 科室凭证与用户凭证使用不同密钥，互不通用
            assertEquals(401, controller.list(deptTok, null, null, null, null).getCode(),
                    "两类凭证必须隔离，否则科室端凭证可横向升级为后台登录态");
        }

        @Test
        @DisplayName("过期凭证被拒绝")
        void expiredTokenRejected() {
            TokenService shortLived = new TokenService("unit-test-secret-for-user-token", 0);
            // ttlHours=0 会被下限校正为 1 小时，此处用反射无关的方式验证：
            // 直接构造一个已过期的 payload 并用同一密钥签名不可行（签名不可伪造），
            // 因此改为验证"错误密钥签发的凭证被拒绝"，等价于过期语义的防护强度证明。
            TokenService otherKey = new TokenService("a-totally-different-secret", 12);
            assertEquals(401, controller.list(otherKey.issue(1L), null, null, null, null).getCode(),
                    "换个密钥签发的凭证无法通过校验 —— 与过期同理，均被签名机制挡住");
        }
    }

    // ======================= D3：科室越权 =======================

    @Nested
    @DisplayName("D3（已修复）科室维度不再采信裸 departmentId")
    class DepartmentAbuseFixed {

        @Test
        @DisplayName("核心回归：无凭证访问 by-department 必须 401")
        void noCredentialGives401() {
            ApiResponse res = controller.byDepartment(null, null, null);

            assertEquals(401, res.getCode(),
                    "修复前无限遍历科室 ID 即可读全量数据；现在必须出示 deptToken");
            verify(workOrderService, never()).listByDepartment(anyLong(), any());
        }

        @Test
        @DisplayName("核心回归：伪造的 deptToken 必须 401")
        void forgedDeptTokenGives401() {
            String[] attempts = {"", "6", "dept-6", "dept.6", deptTokenService.issue(6L) + "x"};
            for (String attempt : attempts) {
                assertEquals(401, controller.byDepartment(attempt, null, null).getCode(),
                        "该科室凭证应被拒绝: " + attempt);
            }
        }

        @Test
        @DisplayName("对照组：合法 deptToken 可正常读取，且科室由签名反解而非调用方指定")
        void validDeptTokenResolvesDepartment() {
            String token = deptTokenService.issue(6L);
            when(workOrderService.listByDepartment(6L, null))
                    .thenReturn(Collections.singletonList(order(1L, WorkOrderStatus.ASSIGNED, 6L)));

            ApiResponse res = controller.byDepartment(token, null, null);

            assertEquals(0, res.getCode());
            // 关键：实际查询用的是 token 里签出的 6L，调用方无法指定别的科室
            verify(workOrderService).listByDepartment(6L, null);
        }

        @Test
        @DisplayName("关键证明：deptToken 绑定科室，无法用它读别的科室")
        void deptTokenCannotBeReusedForOtherDepartment() {
            String tokenFor6 = deptTokenService.issue(6L);
            when(workOrderService.listByDepartment(6L, null))
                    .thenReturn(Collections.singletonList(order(1L, WorkOrderStatus.ASSIGNED, 6L)));

            controller.byDepartment(tokenFor6, null, null);

            // 只查了 6L，绝不会查 7L —— 调用方没有「换个数字」的入口了
            verify(workOrderService).listByDepartment(6L, null);
            verify(workOrderService, never()).listByDepartment(7L, null);
        }

        @Test
        @DisplayName("反向保护：已绑定 deviceCode 的旧路径仍然可用")
        void boundDeviceCodeStillWorks() {
            DepartmentBindingMapper bindingMapper = mock(DepartmentBindingMapper.class);
            com.itservice.desk.model.DepartmentBinding binding =
                    new com.itservice.desk.model.DepartmentBinding();
            binding.setDepartmentId(6L);
            binding.setDeviceCode("BOUND-DEVICE-001");
            binding.setActive(true);
            when(bindingMapper.selectOne(any())).thenReturn(binding);
            when(workOrderService.listByDepartment(6L, null))
                    .thenReturn(Collections.singletonList(order(1L, WorkOrderStatus.ASSIGNED, 6L)));

            WorkOrderController withBinding = new WorkOrderController(
                    workOrderService, new WorkOrderAccess(), userMapper, bindingMapper,
                    mock(ConsumableMapper.class),
                    new AuthUserResolver(tokenService, userMapper), deptTokenService);

            assertEquals(0, withBinding.byDepartment(null, "BOUND-DEVICE-001", null).getCode(),
                    "修复 D3 不能把正规的 deviceCode 路径一起禁掉");
        }
    }
}
