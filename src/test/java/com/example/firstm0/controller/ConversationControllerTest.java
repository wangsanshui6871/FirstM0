package com.example.firstm0.controller;

import com.example.firstm0.auth.JwtService;
import com.example.firstm0.common.ConversationNotFoundException;
import com.example.firstm0.domain.MessageRole;
import com.example.firstm0.dto.ConversationSummary;
import com.example.firstm0.dto.MessageResponse;
import com.example.firstm0.service.ChatService;
import com.example.firstm0.service.ConversationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ConversationController 的接口测试：验证 URL、返回格式、权限相关的 404 和 401。
 */
@WebMvcTest(ConversationController.class)
class ConversationControllerTest {

    private static final String TOKEN = "test-token";
    private static final String AUTH_HEADER = "Bearer " + TOKEN;
    private static final Long USER_ID = 1L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ChatService chatService;

    @MockitoBean
    private ConversationService conversationService;

    @MockitoBean
    private JwtService jwtService;

    /** 会话列表：返回数组，按更新时间倒序由 Service 保证。 */
    @Test
    void returnsConversationList() throws Exception {
        when(jwtService.parseUserId(TOKEN)).thenReturn(USER_ID);
        when(conversationService.list(USER_ID)).thenReturn(List.of(
                new ConversationSummary(3L, "退换货咨询",
                        LocalDateTime.of(2026, 11, 17, 10, 0),
                        LocalDateTime.of(2026, 11, 17, 10, 5))
        ));

        mockMvc.perform(get("/api/conversations").header("Authorization", AUTH_HEADER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(3))
                .andExpect(jsonPath("$[0].title").value("退换货咨询"));
    }

    /** 正常查历史：返回消息数组，按时间从早到晚。 */
    @Test
    void returnsMessagesInOrder() throws Exception {
        when(jwtService.parseUserId(TOKEN)).thenReturn(USER_ID);
        LocalDateTime time = LocalDateTime.of(2026, 11, 17, 10, 0);

        when(chatService.listMessages(USER_ID, 1L)).thenReturn(List.of(
                new MessageResponse(1L, MessageRole.USER, "你好", time),
                new MessageResponse(2L, MessageRole.ASSISTANT, "你好，我是客服助手", time)
        ));

        mockMvc.perform(get("/api/conversations/1/messages").header("Authorization", AUTH_HEADER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].role").value("USER"))
                .andExpect(jsonPath("$[1].role").value("ASSISTANT"));
    }

    /** 会话不存在或不属于当前用户：都返回 404。 */
    @Test
    void returnsNotFoundForUnknownConversation() throws Exception {
        when(jwtService.parseUserId(TOKEN)).thenReturn(USER_ID);
        when(chatService.listMessages(USER_ID, 99L)).thenThrow(new ConversationNotFoundException(99L));

        mockMvc.perform(get("/api/conversations/99/messages").header("Authorization", AUTH_HEADER))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CONVERSATION_NOT_FOUND"));
    }

    /** 重命名成功：204，没有响应体。 */
    @Test
    void renamesConversation() throws Exception {
        when(jwtService.parseUserId(TOKEN)).thenReturn(USER_ID);

        mockMvc.perform(patch("/api/conversations/1")
                        .header("Authorization", AUTH_HEADER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "退换货咨询"}
                                """))
                .andExpect(status().isNoContent());
    }

    /** 重命名时标题为空：400。 */
    @Test
    void rejectsBlankTitle() throws Exception {
        when(jwtService.parseUserId(TOKEN)).thenReturn(USER_ID);

        mockMvc.perform(patch("/api/conversations/1")
                        .header("Authorization", AUTH_HEADER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "  "}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    /** 删除成功：204。 */
    @Test
    void deletesConversation() throws Exception {
        when(jwtService.parseUserId(TOKEN)).thenReturn(USER_ID);

        mockMvc.perform(delete("/api/conversations/1").header("Authorization", AUTH_HEADER))
                .andExpect(status().isNoContent());
    }

    /** 删除别人的会话：404，而不是 403——不告诉对方这个 id 到底存不存在。 */
    @Test
    void returnsNotFoundWhenDeletingOthersConversation() throws Exception {
        when(jwtService.parseUserId(TOKEN)).thenReturn(USER_ID);
        doThrow(new ConversationNotFoundException(99L))
                .when(conversationService).delete(eq(USER_ID), eq(99L));

        mockMvc.perform(delete("/api/conversations/99").header("Authorization", AUTH_HEADER))
                .andExpect(status().isNotFound());
    }

    /** 没带 token 访问会话列表：401。 */
    @Test
    void rejectsRequestWithoutToken() throws Exception {
        mockMvc.perform(get("/api/conversations"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("TOKEN_INVALID"));
    }
}
