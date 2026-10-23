package com.example.firstm0.dto;

/**
 * /api/hello 的响应对象（DTO —— 数据传输对象）。
 *
 * <p>为什么用 record：record 是 Java 16+ 的语法，编译时会自动生成
 * 构造器、取值方法、equals、hashCode、toString，省掉一堆模板代码。
 *
 * <p>怎么变成 JSON：Spring 默认用 Jackson 序列化，
 * 它会调用 record 的取值方法 {@code message()} 拿到值，
 * 最终输出 {@code {"message":"..."}}。
 */
public record HelloResponse(String message) {
    // record 不用手写字段和 getter：括号里的参数就是字段，message() 就是取值方法
}