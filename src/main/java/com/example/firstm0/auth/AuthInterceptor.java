package com.example.firstm0.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 登录校验拦截器。
 *
 * <p>工作流程：
 * <ol>
 *   <li>从请求头拿 {@code Authorization: Bearer <token>}；</li>
 *   <li>交给 {@link JwtService} 校验签名和有效期，取出 userId；</li>
 *   <li>把 userId 放进 request attribute，后面的 Controller 直接取用；</li>
 *   <li>任何一步失败就抛 {@link TokenInvalidException}，由全局异常处理统一返回 401。</li>
 * </ol>
 *
 * <p>为什么不把 userId 存进 session：
 * JWT 是无状态的，服务端不需要记住谁登录过。
 * 好处是以后部署多台机器也不用做 session 共享；
 * 代价是 token 签发后在过期前无法主动作废（要作废就得引入黑名单）。
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;

    public AuthInterceptor(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 只有真正打到 Controller 方法的请求才校验；
        // 静态资源、错误页这些 handler 不是 HandlerMethod，直接放行
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            throw new TokenInvalidException("请先登录");
        }

        String token = header.substring(BEARER_PREFIX.length()).trim();

        if (token.isEmpty()) {
            throw new TokenInvalidException("请先登录");
        }

        Long userId = jwtService.parseUserId(token);
        request.setAttribute(AuthAttributes.USER_ID, userId);

        return true;
    }
}
