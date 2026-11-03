package com.example.firstm0.common;

/**
 * 会话不存在异常。
 *
 * <p>当请求里带了 conversationId，但数据库里查不到这个会话时抛出。
 * 由 GlobalExceptionHandler 统一转成 404，而不是 500。
 */
public class ConversationNotFoundException extends RuntimeException {

    public ConversationNotFoundException(Long conversationId) {
        super("会话不存在：" + conversationId);
    }
}
