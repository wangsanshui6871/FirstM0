package com.example.firstm0.domain;

/**
 * 用户角色。
 *
 * <p>M1.1 只用得上 USER；ADMIN 留给后面的管理后台（M3）。
 * 提前定义好，以后加权限判断不用再改表结构。
 */
public enum UserRole {
    /** 普通用户 */
    USER,
    /** 管理员，可以看工单、管知识库 */
    ADMIN
}
