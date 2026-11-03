package com.example.firstm0.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * POST /api/chat 的请求体。
 *
 * <p>对应的 JSON 形如：
 * <pre>
 * { "conversationId": null, "message": "你好" }
 * </pre>
 *
 * <p>字段说明：
 * <ul>
 *   <li>conversationId：会话 id。第一次聊天传 null，服务端会新建会话并返回真实 id；
 *       继续同一段对话时传上一次返回的 id，服务端会往同一个会话里追加消息。</li>
 *   <li>message：用户这次说的话，必填，最长 2000 字符。</li>
 * </ul>
 *
 * <p>校验注解本身不会自动生效，必须在 Controller 的参数上再加 @Valid 才会触发。
 */
public record ChatRequest(
        // 会话 id，可以为 null（第一次聊天时由服务端新建会话）
        Long conversationId,

        // @NotBlank：不能为 null、不能是空串、不能全是空格
        @NotBlank(message = "消息不能为空")
        // @Size：长度上限 2000 字符；max 只管上限，这里配合 @NotBlank 已隐含“至少 1 个字符”
        @Size(max = 2000, message = "消息最长 2000 字符")
        String message
) {
}
