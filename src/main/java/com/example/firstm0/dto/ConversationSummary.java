package com.example.firstm0.dto;

import com.example.firstm0.domain.Conversation;

import java.time.LocalDateTime;

/**
 * 会话列表里的一项。
 *
 * <p>列表不需要消息内容，只需要“怎么显示一行”：
 * 标题、最后更新时间、以及点进去时用的 id。
 *
 * <p>为什么不直接返回实体：实体有 userId 这类内部字段，
 * 而且以后表结构变了接口就会跟着变，单独定义 DTO 更稳。
 */
public record ConversationSummary(
        Long id,
        String title,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static ConversationSummary from(Conversation conversation) {
        return new ConversationSummary(
                conversation.getId(),
                conversation.getTitle(),
                conversation.getCreatedAt(),
                conversation.getUpdatedAt()
        );
    }
}
