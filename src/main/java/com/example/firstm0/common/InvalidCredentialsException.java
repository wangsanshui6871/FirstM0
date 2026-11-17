package com.example.firstm0.common;

/**
 * 登录失败：用户名不存在或密码不对，最终返回 401。
 *
 * <p>故意不区分“用户不存在”和“密码错误”，
 * 统一提示“用户名或密码错误”，避免攻击者用它来探测哪些用户名已注册。
 */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("用户名或密码错误");
    }
}
