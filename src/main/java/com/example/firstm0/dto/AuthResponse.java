package com.example.firstm0.dto;

import com.example.firstm0.domain.UserRole;

/**
 * 注册/登录成功后返回的内容。
 *
 * <p>注意这里没有 passwordHash —— 密码相关字段永远不出现在响应里。
 *
 * @param token    JWT，前端存起来，之后请求放在 Authorization 头里
 * @param userId   用户 id
 * @param username 用户名
 * @param role     角色，M1.1 都是 USER
 */
public record AuthResponse(
        String token,
        Long userId,
        String username,
        UserRole role
) {
}
