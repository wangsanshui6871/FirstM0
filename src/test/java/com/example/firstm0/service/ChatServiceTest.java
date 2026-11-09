package com.example.firstm0.service;

import com.example.firstm0.common.ConversationNotFoundException;
import com.example.firstm0.domain.Message;
import com.example.firstm0.domain.MessageRole;
import com.example.firstm0.dto.ChatResponse;
import com.example.firstm0.llm.LlmClient;
import com.example.firstm0.llm.LlmException;
import com.example.firstm0.repository.MessageRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * ChatService 的业务测试。
 *
 * <p>@SpringBootTest 启动完整上下文（JPA + H2）；
 * 但大模型用 @MockitoBean 换掉，测试不会真的发网络请求、不会花钱、也不需要 Key。
 *
 * <p>@Transactional 加在测试类上：每个测试方法结束后自动回滚，测试数据不残留。
 */
@SpringBootTest
@Transactional
class ChatServiceTest {

    @Autowired
    private ChatService chatService;

    @Autowired
    private MessageRepository messageRepository;

    /** 假的模型客户端，由每个测试自己决定返回什么。 */
    @MockitoBean
    private LlmClient llmClient;

    /**
     * 第一次聊天：应新建会话，保存“用户消息 + 模型回复”两条记录。
     */
    @Test
    void createsConversationAndSavesTwoMessages() {
        when(llmClient.chat(anyString(), anyString())).thenReturn("你好，我是客服助手，有什么可以帮你？");

        ChatResponse response = chatService.chat(null, "你好");

        assertThat(response.conversationId()).isNotNull();
        assertThat(response.reply()).isEqualTo("你好，我是客服助手，有什么可以帮你？");

        List<Message> messages = messageRepository.findByConversationIdOrderByIdAsc(response.conversationId());
        assertThat(messages).hasSize(2);
        assertThat(messages.get(0).getRole()).isEqualTo(MessageRole.USER);
        assertThat(messages.get(0).getContent()).isEqualTo("你好");
        assertThat(messages.get(1).getRole()).isEqualTo(MessageRole.ASSISTANT);
        assertThat(messages.get(1).getContent()).isEqualTo("你好，我是客服助手，有什么可以帮你？");
    }

    /**
     * 带着已有 conversationId 再聊一句：不应新建会话，而是往同一个会话里追加两条消息。
     */
    @Test
    void appendsMessagesToExistingConversation() {
        when(llmClient.chat(anyString(), anyString())).thenReturn("好的");

        ChatResponse first = chatService.chat(null, "你好");
        ChatResponse second = chatService.chat(first.conversationId(), "再见");

        assertThat(second.conversationId()).isEqualTo(first.conversationId());

        List<Message> messages = messageRepository.findByConversationIdOrderByIdAsc(first.conversationId());
        assertThat(messages).hasSize(4);
        assertThat(messages.get(2).getContent()).isEqualTo("再见");
    }

    /**
     * 模型调用失败：不应把异常抛给前端，而是返回兜底话术，
     * 并且这条兜底话术也要落库，保证历史记录完整。
     */
    @Test
    void returnsFallbackReplyWhenLlmFails() {
        when(llmClient.chat(anyString(), anyString()))
                .thenThrow(new LlmException("模型调用失败：timeout"));

        ChatResponse response = chatService.chat(null, "你们支持退货吗");

        assertThat(response.reply()).contains("人工客服");

        List<Message> messages = messageRepository.findByConversationIdOrderByIdAsc(response.conversationId());
        assertThat(messages).hasSize(2);
        assertThat(messages.get(1).getRole()).isEqualTo(MessageRole.ASSISTANT);
        assertThat(messages.get(1).getContent()).contains("人工客服");
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
