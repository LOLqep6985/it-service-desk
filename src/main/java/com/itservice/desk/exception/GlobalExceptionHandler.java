package com.itservice.desk.exception;

import com.itservice.desk.dto.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.validation.ConstraintViolationException;

/**
 * 全局异常处理器（缺陷 D1 修复）。
 *
 * <h2>修复前的问题</h2>
 * 项目内无任何 {@code @ControllerAdvice}，业务异常（如「工单不存在」）会一路抛到
 * Servlet 容器，被渲染成 Spring 默认错误页：
 * <pre>
 *   {"timestamp":...,"status":500,"error":"Internal Server Error","path":"/api/work-orders/99999"}
 * </pre>
 * 前端 {@code api.js} 依赖 {@code json.code} 分支处理错误，遇到该结构会走
 * 「服务端返回异常（HTTP 500）」兜底分支，无法给出准确提示。
 *
 * <h2>修复策略</h2>
 * 不逐端点补 try-catch（10 个端点同模式，易漏），改为统一兜底：
 * <ul>
 *   <li>{@link NotFoundException} → <b>HTTP 404</b> + {@code code=404}，语义正确；</li>
 *   <li>{@link IllegalArgumentException} → {@code code=500}，<b>保持既有契约</b>
 *       （前端与既有测试均按此码判断，不擅自改成 400 以免造成行为漂移）；</li>
 *   <li>其它未预期异常 → {@code code=500}，记录堆栈便于排查。</li>
 * </ul>
 *
 * <p>注意所有分支都返回<b>统一 {@code ApiResponse} 结构</b>，
 * 前端 {@code json.code !== 0} 分支即可正常提示，不再出现"返回异常（HTTP 500）"。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** 资源不存在 → 404，语义与 REST 规范一致 */
    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse handleNotFound(NotFoundException e) {
        log.debug("资源不存在: {}", e.getMessage());
        return ApiResponse.error(404, e.getMessage());
    }

    /**
     * 业务校验失败（状态不允许、参数不合法等）→ 保持原有 500 业务码，
     * 避免把既有前端文案与断言全部打乱。
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ApiResponse handleIllegalArgument(IllegalArgumentException e) {
        log.debug("业务校验失败: {}", e.getMessage());
        return ApiResponse.error(500, e.getMessage());
    }

    /** 未预期异常：对外返回统一结构，对内保留完整堆栈 */
    @ExceptionHandler(Exception.class)
    public ApiResponse handleUnexpected(Exception e) {
        log.error("未预期异常", e);
        return ApiResponse.error(500, "服务器内部错误，请稍后重试");
    }

    /**
     * Bean Validation 入参校验失败（待办一新增）。
     *
     * <p>修复前：DTO 无校验注解，空 {@code reporterName}/{@code deviceName} 能直接落库。
     * 修复后：由 {@code @Valid} 触发，统一转成 400 + 首条字段错误，
     * 让前端能直接展示"哪个字段不合格"，而不是收到 500 或静默成功。
     *
     * <p>返回 {@code code=400} 与"资源不存在(404)"、"业务规则(500)"区分开，
     * 便于前端按码分流：400 提示用户改输入，500 提示操作不被允许。
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse handleValidation(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getDefaultMessage() == null
                        ? f.getField() + " 不合法"
                        : f.getDefaultMessage())
                .distinct()
                .collect(java.util.stream.Collectors.joining("；"));
        if (msg.isBlank()) {
            msg = "请求参数不合法";
        }
        log.debug("参数校验失败: {}", msg);
        return ApiResponse.error(400, msg);
    }

    /** 单参数/路径变量校验失败（如 @RequestParam 上的约束） */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse handleConstraintViolation(ConstraintViolationException e) {
        String msg = e.getConstraintViolations().stream()
                .map(v -> v.getMessage() == null ? v.getPropertyPath() + " 不合法" : v.getMessage())
                .distinct()
                .collect(java.util.stream.Collectors.joining("；"));
        log.debug("参数约束失败: {}", msg);
        return ApiResponse.error(400, msg.isBlank() ? "请求参数不合法" : msg);
    }

    /** 请求体 JSON 无法解析（如类型不匹配）→ 400，而不是 500 */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse handleUnreadableBody(HttpMessageNotReadableException e) {
        log.debug("请求体解析失败: {}", e.getMessage());
        return ApiResponse.error(400, "请求体格式错误，请检查提交内容");
    }
}
