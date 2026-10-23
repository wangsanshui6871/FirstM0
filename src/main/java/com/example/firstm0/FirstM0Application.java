package com.example.firstm0;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 应用启动类 —— 整个项目的入口。
 *
 * <p>职责：
 * <ul>
 *   <li>作为 Spring Boot 的启动点：main 方法一执行，内嵌的 Tomcat 就会监听 8080 端口。</li>
 *   <li>@SpringBootApplication 开启「自动配置 + 组件扫描」，
 *       扫描范围是包 com.example.firstm0 及其所有子包；
 *       所以 controller / service 等包里的类，只要加了注解就会被自动装进 Spring 容器。</li>
 * </ul>
 */
@SpringBootApplication
public class FirstM0Application {

    public static void main(String[] args) {
        // 启动 Spring 容器：创建 Bean、启动内嵌 Tomcat、加载 application.yml
        SpringApplication.run(FirstM0Application.class, args);
    }
}