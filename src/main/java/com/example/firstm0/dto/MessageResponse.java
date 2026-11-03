package com.example.firstm0.dto;

import com.example.firstm0.domain.Message;
import com.example.firstm0.domain.MessageRole;

import java.time.LocalDateTime;

/**
 * 一条历史消息的响应格式。
 *
 * <p>为什么不直接返回 {@link Message} 实体？
 * 因为实体是数据库结构，字段名、关联关系以后都可能变；
 * 单独定义 DTO 可以让接口格式稳定，也能只暴露前端需要的字段。
 *
 * <p>JSON 形如：
 * <pre>
 * { "id": 1, "role": "USER", "content": "你好", "createdAt": "2026-11-03T10:00:00" }
 * </pre>
 */
public record MessageResponse(
        Long id,
        MessageRole role,
        String content,
        LocalDateTime createdAt
) {

    /** 把实体转成 DTO，放在这里是为了让转换逻辑只写一次。 */
    public static MessageResponse from(Message message) {
        return new MessageResponse(
                message.getId(),
                message.getRole(),
                message.getContent(),
                message.getCreatedAt()
        );
    }
}
