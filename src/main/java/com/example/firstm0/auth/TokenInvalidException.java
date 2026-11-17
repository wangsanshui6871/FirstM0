package com.example.firstm0.auth;

/**
 * token 无效时抛出：格式错、签名不对、已过期都走这里。
 *
 * <p>由全局异常处理转成 401，前端收到后应该清掉本地 token 并要求重新登录。
 */
public class TokenInvalidException extends RuntimeException {

    public TokenInvalidException(String message) {
        super(message);
    }
}
