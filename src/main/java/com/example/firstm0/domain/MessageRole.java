package com.example.firstm0.domain;

/**
 * 消息角色：这条消息是谁说的。
 *
 * <p>数据库里存的是字符串 "USER" / "ASSISTANT"，不是数字。
 * 因为枚举用 {@code @Enumerated(EnumType.STRING)} 存字符串，
 * 以后在数据库里直接看数据也能一眼看懂，不用查“0 是谁、1 是谁”。
 */
public enum MessageRole {
    /** 用户说的话 */
    USER,
    /** 客服（机器人）说的话 */
    ASSISTANT
}
