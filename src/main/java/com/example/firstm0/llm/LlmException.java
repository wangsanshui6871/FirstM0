package com.example.firstm0.llm;

/**
 * 大模型调用失败时抛出的异常。
 *
 * <p>把各种底层异常（HTTP 401、连接超时、返回为空……）统一收成一种，
 * 上层 Service 只需要 catch 一次，就能决定怎么降级。
 *
 * <p>message 里只写“能安全展示”的原因，不放 API Key、不放完整响应体。
 */
public class LlmException extends RuntimeException {

    public LlmException(String message) {
        super(message);
    }

    public LlmException(String message, Throwable cause) {
        super(message, cause);
    }
}
