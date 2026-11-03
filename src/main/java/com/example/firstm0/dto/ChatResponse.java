package com.example.firstm0.dto;

/**
 * POST /api/chat 的响应体。
 *
 * <p>对应的 JSON 形如：
 * <pre>
 * { "conversationId": 1, "reply": "我收到了：你好" }
 * </pre>
 *
 * <p>M0.2 起 conversationId 是数据库生成的真实 id：
 * 前端拿到后存起来，下次请求带上来就能续上同一段对话。
 */
public record ChatResponse(
        // 会话 id：服务端新建或复用的会话
        Long conversationId,

        // 机器人这次的回答内容
        String reply
) {
}
