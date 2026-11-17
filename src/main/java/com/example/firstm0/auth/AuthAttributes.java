package com.example.firstm0.auth;

/**
 * 当前登录用户在这条请求里的 key。
 *
 * <p>拦截器校验完 token 后，把 userId 放进 request attribute；
 * Controller 用 {@code @RequestAttribute} 取出来。
 * 抽成常量是为了避免两处手写字符串写错。
 */
public final class AuthAttributes {

    /** request attribute 的名字：当前登录用户 id。 */
    public static final String USER_ID = "currentUserId";

    private AuthAttributes() {
    }
}
