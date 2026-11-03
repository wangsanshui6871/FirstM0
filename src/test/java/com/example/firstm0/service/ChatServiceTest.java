package com.example.firstm0.service;

import com.example.firstm0.common.ConversationNotFoundException;
import com.example.firstm0.domain.Message;
import com.example.firstm0.domain.MessageRole;
import com.example.firstm0.dto.ChatResponse;
import com.example.firstm0.repository.MessageRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * ChatService 的数据库测试。
 *
 * <p>@SpringBootTest 会启动完整的 Spring 上下文（包括 JPA 和 H2 内存数据库），
 * 这样测的是真实的“存库”行为，不是假数据。
 *
 * <p>@Transactional 加在测试类上：每个测试方法结束后自动回滚，
 * 测试数据不会留在库里，也不会互相干扰。
 */
@SpringBootTest
@Transactional
class ChatServiceTest {

    @Autowired
    private ChatService chatService;

    @Autowired
    private MessageRepository messageRepository;

    /**
     * 第一次聊天：应新建会话，并保存“用户消息 + 机器人回复”两条记录。
     */
    @Test
    void createsConversationAndSavesTwoMessages() {
        ChatResponse response = chatService.chat(null, "你好");

        assertThat(response.conversationId()).isNotNull();
        assertThat(response.reply()).isEqualTo("我收到了：你好");

        List<Message> messages = messageRepository.findByConversationIdOrderByIdAsc(response.conversationId());
        assertThat(messages).hasSize(2);
        assertThat(messages.get(0).getRole()).isEqualTo(MessageRole.USER);
        assertThat(messages.get(0).getContent()).isEqualTo("你好");
        assertThat(messages.get(1).getRole()).isEqualTo(MessageRole.ASSISTANT);
        assertThat(messages.get(1).getContent()).isEqualTo("我收到了：你好");
    }

    /**
     * 带着已有 conversationId 再聊一句：不应新建会话，而是往同一个会话里追加两条消息。
     */
    @Test
    void appendsMessagesToExistingConversation() {
        ChatResponse first = chatService.chat(null, "你好");
        ChatResponse second = chatService.chat(first.conversationId(), "再见");

        assertThat(second.conversationId()).isEqualTo(first.conversationId());

        List<Message> messages = messageRepository.findByConversationIdOrderByIdAsc(first.conversationId());
        assertThat(messages).hasSize(4);
        assertThat(messages.get(2).getContent()).isEqualTo("再见");
    }

    /**
     * 传了不存在的会话 id：应抛出 ConversationNotFoundException（最终由全局异常处理转成 404）。
     */
    @Test
    void throwsWhenConversationDoesNotExist() {
        assertThatThrownBy(() -> chatService.chat(999L, "在吗"))
                .isInstanceOf(ConversationNotFoundException.class);
    }

    /**
     * 查不存在会话的历史：同样应抛异常，而不是返回空列表。
     */
    @Test
    void listMessagesThrowsWhenConversationDoesNotExist() {
        assertThatThrownBy(() -> chatService.listMessages(999L))
                .isInstanceOf(ConversationNotFoundException.class);
    }
}
