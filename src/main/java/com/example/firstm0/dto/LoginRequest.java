package com.example.firstm0.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 登录请求体。
 *
 * <p>登录时不校验用户名/密码的长度和格式：
 * 老用户的密码规则可能和现在不同，格式校验只会让登录莫名其妙失败。
 * 这里只要求非空，是否正确交给 Service 去比对。
 */
public record LoginRequest(
        @NotBlank(message = "用户名不能为空")
        String username,

        @NotBlank(message = "密码不能为空")
        String password
) {
}
