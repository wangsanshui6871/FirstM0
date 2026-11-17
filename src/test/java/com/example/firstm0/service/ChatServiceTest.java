package com.example.firstm0.service;

import com.example.firstm0.common.ConversationNotFoundException;
import com.example.firstm0.domain.Conversation;
import com.example.firstm0.domain.Message;
import com.example.firstm0.domain.MessageRole;
import com.example.firstm0.dto.ChatResponse;
import com.example.firstm0.llm.LlmClient;
import com.example.firstm0.llm.LlmException;
import com.example.firstm0.llm.LlmMessage;
import com.example.firstm0.repository.ConversationRepository;
import com.example.firstm0.repository.MessageRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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

    /** 当前登录用户 */
    private static final Long USER_ID = 1L;

    /** 另一个用户，用来验证“看不到别人的会话” */
    private static final Long OTHER_USER_ID = 2L;

    @Autowired
    private ChatService chatService;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private ConversationRepository conversationRepository;

    /** 假的模型客户端，由每个测试自己决定返回什么。 */
    @MockitoBean
    private LlmClient llmClient;

    /**
     * 第一次聊天：应新建会话（归属当前用户），保存“用户消息 + 模型回复”两条记录。
     */
    @Test
    void createsConversationAndSavesTwoMessages() {
        when(llmClient.chat(anyList())).thenReturn("你好，我是客服助手，有什么可以帮你？");

        ChatResponse response = chatService.chat(USER_ID, null, "你好");

        assertThat(response.conversationId()).isNotNull();
        assertThat(response.reply()).isEqualTo("你好，我是客服助手，有什么可以帮你？");

        Conversation conversation = conversationRepository.findById(response.conversationId()).orElseThrow();
        assertThat(conversation.getUserId()).isEqualTo(USER_ID);

        List<Message> messages = messageRepository.findByConversationIdOrderByIdAsc(response.conversationId());
        assertThat(messages).hasSize(2);
        assertThat(messages.get(0).getRole()).isEqualTo(MessageRole.USER);
        assertThat(messages.get(0).getContent()).isEqualTo("你好");
        assertThat(messages.get(1).getRole()).isEqualTo(MessageRole.ASSISTANT);
    }

    /**
     * 带着已有 conversationId 再聊一句：不应新建会话，而是往同一个会话里追加两条消息。
     */
    @Test
    void appendsMessagesToExistingConversation() {
        when(llmClient.chat(anyList())).thenReturn("好的");

        ChatResponse first = chatService.chat(USER_ID, null, "你好");
        ChatResponse second = chatService.chat(USER_ID, first.conversationId(), "再见");

        assertThat(second.conversationId()).isEqualTo(first.conversationId());

        List<Message> messages = messageRepository.findByConversationIdOrderByIdAsc(first.conversationId());
        assertThat(messages).hasSize(4);
        assertThat(messages.get(2).getContent()).isEqualTo("再见");
    }

    /**
     * M1.2 的核心安全边界：往别人的会话里发消息，必须当成“会话不存在”。
     */
    @Test
    void rejectsChattingInOthersConversation() {
        when(llmClient.chat(anyList())).thenReturn("好的");

        ChatResponse created = chatService.chat(USER_ID, null, "你好");

        assertThatThrownBy(() -> chatService.chat(OTHER_USER_ID, created.conversationId(), "我也来插一句"))
                .isInstanceOf(ConversationNotFoundException.class);
    }

    /**
     * 查询别人的会话历史，同样返回 404，而不是把内容给出去。
     */
    @Test
    void rejectsReadingOthersConversation() {
        when(llmClient.chat(anyList())).thenReturn("好的");

        ChatResponse created = chatService.chat(USER_ID, null, "你好");

        assertThatThrownBy(() -> chatService.listMessages(OTHER_USER_ID, created.conversationId()))
                .isInstanceOf(ConversationNotFoundException.class);
    }

    /**
     * M0.4 的核心：第二轮请求必须把第一轮的内容一起发给模型，
     * 否则模型读不懂“那运费谁出”里的“那”指什么。
     */
    @Test
    void sendsPreviousTurnsToModel() {
        when(llmClient.chat(anyList())).thenReturn("7 天无理由退货");

        ChatResponse first = chatService.chat(USER_ID, null, "你们支持退货吗");
        chatService.chat(USER_ID, first.conversationId(), "那运费谁出");

        List<LlmMessage> prompt = capturePromptOfSecondCall();

        // system + 第一轮用户 + 第一轮回复 + 当前问题 = 4 条
        assertThat(prompt).hasSize(4);
        assertThat(prompt.get(0).role()).isEqualTo("system");
        assertThat(prompt.get(1).content()).isEqualTo("你们支持退货吗");
        assertThat(prompt.get(2).role()).isEqualTo("assistant");
        assertThat(prompt.get(2).content()).isEqualTo("7 天无理由退货");
        assertThat(prompt.get(3).content()).isEqualTo("那运费谁出");
    }

    /**
     * 历史总字符超过 4000 时，应从最早的消息开始丢，保证成本可控。
     *
     * <p>构造 5 条 1001 字的历史，加上当前问题 4 字：
     * 从新往旧装，装 3 条历史就超过 4000，所以最终只有
     * system + 3 条历史 + 当前问题 = 5 条。
     */
    @Test
    void dropsOldestMessagesWhenOverCharacterBudget() {
        when(llmClient.chat(anyList())).thenReturn("好");

        LocalDateTime now = LocalDateTime.now();
        Conversation conversation = conversationRepository.save(
                new Conversation(USER_ID, "长对话测试", now));

        for (int i = 0; i < 5; i++) {
            MessageRole role = (i % 2 == 0) ? MessageRole.USER : MessageRole.ASSISTANT;
            messageRepository.save(new Message(
                    conversation.getId(), role, "长".repeat(1000) + i, now));
        }

        chatService.chat(USER_ID, conversation.getId(), "当前问题");

        List<LlmMessage> prompt = captureOnlyPrompt();

        assertThat(prompt).hasSize(5);
        assertThat(prompt.get(0).role()).isEqualTo("system");
        // 最早保留下来的应该是第 3 条历史（下标 2），第 1、2 条被丢掉
        assertThat(prompt.get(1).content()).endsWith("2");
        assertThat(prompt.get(4).content()).isEqualTo("当前问题");

        int totalChars = prompt.stream().skip(1).mapToInt(message -> message.content().length()).sum();
        assertThat(totalChars).isLessThanOrEqualTo(4000 + 4);
    }

    /**
     * 模型调用失败：不应把异常抛给前端，而是返回兜底话术，
     * 并且这条兜底话术也要落库，保证历史记录完整。
     */
    @Test
    void returnsFallbackReplyWhenLlmFails() {
        when(llmClient.chat(anyList())).thenThrow(new LlmException("模型调用失败：timeout"));

        ChatResponse response = chatService.chat(USER_ID, null, "你们支持退货吗");

        assertThat(response.reply()).contains("人工客服");

        List<Message> messages = messageRepository.findByConversationIdOrderByIdAsc(response.conversationId());
        assertThat(messages).hasSize(2);
        assertThat(messages.get(1).getRole()).isEqualTo(MessageRole.ASSISTANT);
        assertThat(messages.get(1).getContent()).contains("人工客服");
    }

    /** 传了不存在的会话 id：抛 ConversationNotFoundException。 */
    @Test
    void throwsWhenConversationDoesNotExist() {
        assertThatThrownBy(() -> chatService.chat(USER_ID, 999L, "在吗"))
                .isInstanceOf(ConversationNotFoundException.class);
    }

    /** 查不存在会话的历史：同样抛异常，而不是返回空列表。 */
    @Test
    void listMessagesThrowsWhenConversationDoesNotExist() {
        assertThatThrownBy(() -> chatService.listMessages(USER_ID, 999L))
                .isInstanceOf(ConversationNotFoundException.class);
    }

    /**
     * 捕获“第二次调用模型时传进去的消息列表”。
     */
    @SuppressWarnings("unchecked")
    private List<LlmMessage> capturePromptOfSecondCall() {
        ArgumentCaptor<List<LlmMessage>> captor = ArgumentCaptor.forClass(List.class);
        verify(llmClient, times(2)).chat(captor.capture());

        return captor.getAllValues().get(1);
    }

    /** 捕获唯一一次模型调用的参数。 */
    @SuppressWarnings("unchecked")
    private List<LlmMessage> captureOnlyPrompt() {
        ArgumentCaptor<List<LlmMessage>> captor = ArgumentCaptor.forClass(List.class);
        verify(llmClient).chat(captor.capture());

        return captor.getValue();
    }
}
