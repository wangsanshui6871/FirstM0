package com.example.firstm0.dto;

/**
 * POST /api/chat 的响应体。
 *
 * <p>对应的 JSON 形如：
 * <pre>
 * { "conversationId": null, "reply": "我收到了：你好" }
 * </pre>
 *
 * <p>M0.1 阶段 conversationId 原样回传（请求里是 null，响应里也是 null）。
 * 到 M0.2 接入数据库后，这里会返回真实生成的会话 id，
 * 前端拿到后存起来，下次请求带上来就能续上同一段对话。
 */
public record ChatResponse(
        // 会话 id：M0.1 直接回传请求里的值
        Long conversationId,

        // 机器人这次的回答内容
        String reply
) {
}
