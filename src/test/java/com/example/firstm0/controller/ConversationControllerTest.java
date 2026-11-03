package com.example.firstm0.controller;

import com.example.firstm0.common.ConversationNotFoundException;
import com.example.firstm0.domain.MessageRole;
import com.example.firstm0.dto.MessageResponse;
import com.example.firstm0.service.ChatService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ConversationController 的接口测试：只验证 URL、返回格式和错误码。
 */
@WebMvcTest(ConversationController.class)
class ConversationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ChatService chatService;

    /**
     * 正常查历史：返回消息数组，按时间从早到晚。
     */
    @Test
    void returnsMessagesInOrder() throws Exception {
        LocalDateTime time = LocalDateTime.of(2026, 11, 3, 10, 0);

        when(chatService.listMessages(1L)).thenReturn(List.of(
                new MessageResponse(1L, MessageRole.USER, "你好", time),
                new MessageResponse(2L, MessageRole.ASSISTANT, "我收到了：你好", time)
        ));

        mockMvc.perform(get("/api/conversations/1/messages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].role").value("USER"))
                .andExpect(jsonPath("$[0].content").value("你好"))
                .andExpect(jsonPath("$[1].role").value("ASSISTANT"))
                .andExpect(jsonPath("$[1].content").value("我收到了：你好"));
    }

    /**
     * 会话不存在：Service 抛异常，全局异常处理应转成 404，而不是 500。
     */
    @Test
    void returnsNotFoundForUnknownConversation() throws Exception {
        when(chatService.listMessages(99L)).thenThrow(new ConversationNotFoundException(99L));

        mockMvc.perform(get("/api/conversations/99/messages"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CONVERSATION_NOT_FOUND"));
    }
}
