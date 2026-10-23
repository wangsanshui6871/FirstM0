package com.example.firstm0.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HelloController 的接口测试。
 *
 * <p>@WebMvcTest 只加载 Web 层（不连数据库、不启动完整上下文），所以跑得快；
 * MockMvc 相当于一个“假浏览器”，可以直接向 Controller 发请求并断言返回值。
 *
 * <p>这类测试的意义：以后改代码时，只要接口行为被改坏了，它就会变红。
 */
@WebMvcTest(HelloController.class)
class HelloControllerTest {

    // Spring 注入的 MockMvc 工具，用来在测试里模拟 HTTP 请求
    @Autowired
    private MockMvc mockMvc;

    @Test
    void returnsHelloMessage() throws Exception {
        mockMvc.perform(get("/api/hello").param("name", "M0"))  // 发 GET /api/hello?name=M0
                .andExpect(status().isOk())                     // 断言：HTTP 状态码是 200
                .andExpect(jsonPath("$.message").value("Hello, M0!"));  // 断言：JSON 里 message 的值
    }
}