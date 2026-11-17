package com.example.firstm0.controller;

import com.example.firstm0.auth.JwtService;
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
 * <p>@WebMvcTest 只加载 Web 层（Controller + 拦截器 + 全局异常处理）。
 * 因此需要：
 * <ul>
 *   <li>@MockitoBean ChatService：隔离数据库和模型；</li>
 *   <li>@MockitoBean JwtService：让 AuthInterceptor 有东西可注入；</li>
 *   <li>每个请求带 Authorization 头：否则会被拦截器拦成 401。</li>
 * </ul>
 */
@WebMvcTest(ChatController.class)
class ChatControllerTest {

    private static final String TOKEN = "test-token";
    private static final String AUTH_HEADER = "Bearer " + TOKEN;
    private static final Long USER_ID = 1L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ChatService chatService;

    @MockitoBean
    private JwtService jwtService;

    /**
     * 正常请求：应返回 200、模型回复，以及真实的 conversationId。
     */
    @Test
    void returnsReplyWithConversationId() throws Exception {
        when(jwtService.parseUserId(TOKEN)).thenReturn(USER_ID);
        when(chatService.chat(USER_ID, null, "你好"))
                .thenReturn(new ChatResponse(1L, "你好，我是客服助手"));

        String body = """
                {"conversationId": null, "message": "你好"}
                """;

        mockMvc.perform(post("/api/chat")
                        .header("Authorization", AUTH_HEADER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply").value("你好，我是客服助手"))
                .andExpect(jsonPath("$.conversationId").value(1));
    }

    /**
     * 没带 token：拦截器应直接拦下，返回 401。
     * 这条用例同时在保护“别人不登录就能白嫖模型”的风险。
     */
    @Test
    void rejectsRequestWithoutToken() throws Exception {
        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"conversationId": null, "message": "你好"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("TOKEN_INVALID"));
    }

    /**
     * 空消息（全是空格）：@NotBlank 应拦下，返回 400。
     */
    @Test
    void rejectsBlankMessage() throws Exception {
        when(jwtService.parseUserId(TOKEN)).thenReturn(USER_ID);

        String body = """
                {"conversationId": null, "message": "   "}
                """;

        mockMvc.perform(post("/api/chat")
                        .header("Authorization", AUTH_HEADER)
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
        when(jwtService.parseUserId(TOKEN)).thenReturn(USER_ID);

        // "a".repeat(2001) 生成 2001 个字符，刚好越过 2000 的上限
        String body = """
                {"conversationId": null, "message": "%s"}
                """.formatted("a".repeat(2001));

        mockMvc.perform(post("/api/chat")
                        .header("Authorization", AUTH_HEADER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("消息最长 2000 字符"));
    }
}
