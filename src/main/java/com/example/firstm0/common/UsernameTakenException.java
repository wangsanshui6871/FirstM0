package com.example.firstm0.common;

/**
 * 用户名已被占用时抛出，最终返回 409（Conflict）。
 *
 * <p>为什么不是 400：请求格式没问题，只是和已有资源冲突，
 * 409 能更准确地表达“这个用户名已经被用了”。
 */
public class UsernameTakenException extends RuntimeException {

    public UsernameTakenException(String username) {
        super("用户名已被占用：" + username);
    }
}
