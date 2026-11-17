package com.example.firstm0.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 注册请求体。
 *
 * <p>校验规则放在这里，Controller 加 @Valid 就会自动生效：
 * <ul>
 *   <li>用户名 3～50 位，只允许字母、数字、下划线</li>
 *   <li>密码 8～64 位（下限 8 是最基本的要求，别再用 123456）</li>
 * </ul>
 */
public record RegisterRequest(
        @NotBlank(message = "用户名不能为空")
        @Size(min = 3, max = 50, message = "用户名长度需在 3～50 之间")
        @Pattern(regexp = "^[A-Za-z0-9_]+$", message = "用户名只能包含字母、数字和下划线")
        String username,

        @NotBlank(message = "密码不能为空")
        @Size(min = 8, max = 64, message = "密码长度需在 8～64 之间")
        String password
) {
}
