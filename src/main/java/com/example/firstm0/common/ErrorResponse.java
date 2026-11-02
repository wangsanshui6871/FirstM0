package com.example.firstm0.common;

/**
 * 统一错误响应体。
 *
 * <p>项目约定（AGENTS.md）：所有错误响应至少包含 code 和 message 两个字段。
 * 前端只需要认这两个字段，不用去猜各接口自己的错误格式。
 *
 * <p>例：
 * <pre>
 * { "code": "VALIDATION_ERROR", "message": "消息不能为空" }
 * </pre>
 */
public record ErrorResponse(String code, String message) {
}
