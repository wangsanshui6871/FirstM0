package com.example.firstm0.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT 相关配置，对应 application.yml 里的 {@code jwt.*}。
 *
 * @param secret       签名密钥，必须至少 32 个字符（HS256 要求 256 位）
 * @param expireHours  token 有效期（小时）
 */
@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
        String secret,
        int expireHours
) {

    /** 紧凑构造器：给开发环境兜底，避免没配的时候启动就崩。 */
    public JwtProperties {
        if (secret == null || secret.isBlank()) {
            // 这只是开发默认值：所有拿到代码的人都知道它，等于没加密。
            // 正式环境必须用环境变量 JWT_SECRET 覆盖。
            secret = "firstm0-dev-only-secret-key-change-me-in-production-2026";
        }
        if (expireHours <= 0) {
            expireHours = 24 * 7;
        }
    }
}
