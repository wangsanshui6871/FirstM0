package com.example.firstm0.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 重命名会话的请求体：{@code {"title": "退换货咨询"}}。
 */
public record RenameConversationRequest(
        @NotBlank(message = "标题不能为空")
        @Size(max = 100, message = "标题最长 100 字符")
        String title
) {
}
