package com.itservice.desk.exception;

/**
 * 资源不存在异常（HTTP 语义应映射为 404）。
 *
 * <p>继承 {@link IllegalArgumentException} 是刻意的兼容性设计：
 * 项目内已有 14 处 {@code catch (IllegalArgumentException)} 与 17 处
 * {@code assertThrows(IllegalArgumentException)}，直接换成新基类会连锁破坏。
 * 作为子类，既能被既有 catch 捕获（行为不变），又能让全局异常处理器
 * 通过类型精确区分"不存在（404）"与"参数/状态非法（400/500）"。
 */
public class NotFoundException extends IllegalArgumentException {

    public NotFoundException(String message) {
        super(message);
    }
}
