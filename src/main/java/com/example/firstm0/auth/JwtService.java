package com.example.firstm0.auth;

import com.example.firstm0.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

/**
 * JWT 的签发与校验。
 *
 * <p>JWT 是什么：一段由三部分组成的字符串 {@code header.payload.signature}，
 * 中间那段 payload 里放着用户 id 和过期时间，最后一段是用密钥算出的签名。
 * 服务端不需要存 session，只要验签通过、没过期，就认为这个 token 可信。
 *
 * <p>注意：payload 只是 Base64 编码，不是加密，任何人都能解开看内容，
 * 所以里面只能放 userId 这种不敏感信息，绝不能放密码。
 */
@Component
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    private final SecretKey key;
    private final Duration ttl;

    public JwtService(JwtProperties properties) {
        // HS256 要求密钥至少 256 位（32 字节），配置里已经保证了长度
        this.key = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
        this.ttl = Duration.ofHours(properties.expireHours());
    }

    /**
     * 签发 token。
     *
     * @param userId   用户 id，放进 subject
     * @param username 用户名，放进自定义 claim，方便前端展示
     * @return 签好名的 JWT 字符串
     */
    public String issue(Long userId, String username) {
        Instant now = Instant.now();

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("username", username)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ttl)))
                .signWith(key)
                .compact();
    }

    /**
     * 校验 token 并取出用户 id。
     *
     * @param token 前端传来的 JWT
     * @return 用户 id
     * @throws TokenInvalidException 签名不对、格式错误、已过期时抛出
     */
    public Long parseUserId(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return Long.valueOf(claims.getSubject());
        } catch (JwtException | IllegalArgumentException exception) {
            // 这里不打印 token 本身，只记原因，避免日志里出现可用的凭证
            log.debug("token 校验失败：{}", exception.getMessage());

            throw new TokenInvalidException("登录状态无效或已过期");
        }
    }
}
