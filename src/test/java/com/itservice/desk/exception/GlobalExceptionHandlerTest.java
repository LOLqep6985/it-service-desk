package com.itservice.desk.exception;

import com.itservice.desk.dto.ApiResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link GlobalExceptionHandler} 单元测试（缺陷 D1 修复的核心机制）。
 *
 * <p>修复前：无任何 {@code @ControllerAdvice}，业务异常抛到容器被渲染成
 * Spring 默认错误页（{@code {"status":500,"error":"Internal Server Error",...}}），
 * 前端无法按 {@code code} 分支处理。
 *
 * <p>修复后：异常统一转成 {@code ApiResponse}，且"资源不存在"映射为 404。
 * 本测试既验证处理逻辑，也验证 {@code @ResponseStatus} 注解确实标了 404
 * （因为单元测试不经过 Spring MVC，HTTP 状态码只能由注解保证）。
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("NotFoundException → code=404 + 原始业务消息")
    void notFoundMapsTo404() {
        ApiResponse res = handler.handleNotFound(new NotFoundException("工单不存在"));

        assertEquals(404, res.getCode());
        assertEquals("工单不存在", res.getMessage());
    }

    @Test
    @DisplayName("核心：@ResponseStatus 必须是 404，否则 HTTP 层仍是 200/500")
    void notFoundHandlerIsAnnotatedWith404() throws NoSuchMethodException {
        Method m = GlobalExceptionHandler.class.getMethod("handleNotFound", NotFoundException.class);
        ResponseStatus rs = m.getAnnotation(ResponseStatus.class);

        assertNotNull(rs, "必须标注 @ResponseStatus，否则 HTTP 状态码不生效");
        assertEquals(HttpStatus.NOT_FOUND, rs.value(),
                "资源不存在应返回 HTTP 404，而不是 500");
    }

    @Test
    @DisplayName("IllegalArgumentException → 保持 code=500，兼容既有前端与断言契约")
    void illegalArgumentKeeps500() {
        ApiResponse res = handler.handleIllegalArgument(
                new IllegalArgumentException("只有已派单的工单可以接单"));

        assertEquals(500, res.getCode(),
                "不能擅自改成 400：既有前端文案与 17 处测试断言都依赖这个码");
        assertEquals("只有已派单的工单可以接单", res.getMessage());
    }

    @Test
    @DisplayName("未预期异常 → 统一结构，且不把内部细节泄露给客户端")
    void unexpectedDoesNotLeakInternals() {
        ApiResponse res = handler.handleUnexpected(
                new NullPointerException("com.itservice.desk.内部类 第 88 行 空指针"));

        assertEquals(500, res.getCode());
        assertNotNull(res.getMessage());
        assertTrue(!res.getMessage().contains("com.itservice.desk"),
                "不应把堆栈/包名等内部信息返回给客户端，实际=" + res.getMessage());
        assertEquals("服务器内部错误，请稍后重试", res.getMessage());
    }

    @Test
    @DisplayName("继承关系校验：NotFoundException 是 IllegalArgumentException 的子类，既有 catch 不受影响")
    void notFoundIsSubclassOfIllegalArgument() {
        NotFoundException e = new NotFoundException("报修单不存在");

        assertTrue(e instanceof IllegalArgumentException,
                "保持子类关系，项目内 14 处 catch(IllegalArgumentException) 与 17 处断言无需修改");
        assertEquals("报修单不存在", e.getMessage());
    }

    @Test
    @DisplayName("所有分支都返回统一 ApiResponse 结构（前端可稳定按 code 分支）")
    void allHandlersReturnUnifiedShape() {
        ApiResponse a = handler.handleNotFound(new NotFoundException("x"));
        ApiResponse b = handler.handleIllegalArgument(new IllegalArgumentException("y"));
        ApiResponse c = handler.handleUnexpected(new RuntimeException("z"));

        for (ApiResponse r : new ApiResponse[]{a, b, c}) {
            assertNotNull(r, "必须返回非 null");
            assertTrue(r.getMessage() != null && !r.getMessage().isBlank(),
                    "message 必须非空，否则前端提示为空");
        }
        assertEquals(0, c.getData() == null ? 0 : 1,
                "错误响应不应携带 data");
    }
}
