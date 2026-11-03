package com.example.firstm0.controller;

import com.example.firstm0.dto.ChatResponse;
import com.example.firstm0.service.ChatService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ChatController 的接口测试。
 *
 * <p>@WebMvcTest 只加载 Web 层（Controller + 全局异常处理），不会加载 Service 和数据库。
 * M0.2 的 Service 依赖 Repository，直接加载会很麻烦，
 * 所以用 @MockitoBean 造一个假的 ChatService，让测试只关心“接口格式对不对”。
 * 真实的数据库存取，放在 ChatServiceTest 里单独测。
 */
@WebMvcTest(ChatController.class)
class ChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    /** 假的 Service：不会真的连数据库。 */
    @MockitoBean
    private ChatService chatService;

    /**
     * 正常请求：应返回 200、固定格式的回复，以及真实的 conversationId。
     */
    @Test
    void returnsReplyWithConversationId() throws Exception {
        // 约定：第一次聊天 conversationId 传 null，Service 返回新建会话的 id
        when(chatService.chat(null, "你好"))
                .thenReturn(new ChatResponse(1L, "我收到了：你好"));

        String body = """
                {"conversationId": null, "message": "你好"}
                """;

        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply").value("我收到了：你好"))
                .andExpect(jsonPath("$.conversationId").value(1));
    }

    /**
     * 空消息（全是空格）：@NotBlank 应拦下，返回 400。
     * 注意：校验失败时根本不会进 Service，所以不需要 stub。
     */
    @Test
    void rejectsBlankMessage() throws Exception {
        String body = """
                {"conversationId": null, "message": "   "}
                """;

        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("消息不能为空"));
    }

    /**
     * 超长消息（2001 字）：@Size 应拦下，返回 400。
     */
    @Test
    void rejectsTooLongMessage() throws Exception {
        // "a".repeat(2001) 生成 2001 个字符，刚好越过 2000 的上限
        String body = """
                {"conversationId": null, "message": "%s"}
                """.formatted("a".repeat(2001));

        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("消息最长 2000 字符"));
    }
}
