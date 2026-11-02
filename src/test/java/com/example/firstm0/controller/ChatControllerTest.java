package com.example.firstm0.controller;

import com.example.firstm0.service.ChatService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ChatController 的接口测试。
 *
 * <p>@WebMvcTest 只加载 Web 层（Controller + 全局异常处理），不会自动加载 @Service，
 * 所以用 @Import(ChatService.class) 手动把业务类加进来。
 * 到 M0.2 接入数据库后，这里会改用 mock 来隔离数据库依赖。
 */
@WebMvcTest(ChatController.class)
@Import(ChatService.class)
class ChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    /**
     * 正常请求：应返回 200，reply 为固定格式，conversationId 为 null。
     */
    @Test
    void returnsFixedReply() throws Exception {
        String body = """
                {"conversationId": null, "message": "你好"}
                """;

        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply").value("我收到了：你好"))
                .andExpect(jsonPath("$.conversationId").value(nullValue()));
    }

    /**
     * 空消息（全是空格）：@NotBlank 应拦下，返回 400。
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
