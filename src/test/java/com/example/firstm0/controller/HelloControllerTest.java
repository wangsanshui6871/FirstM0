package com.example.firstm0.controller;

import com.example.firstm0.auth.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
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

    /**
     * M1.2 起 WebConfig 会注册 AuthInterceptor，而拦截器依赖 JwtService。
     * 这个用例只测 /api/hello（已在拦截器里排除），所以给个空实现就够。
     */
    @MockitoBean
    private JwtService jwtService;

    @Test
    void returnsHelloMessage() throws Exception {
        // /api/hello 被排除在登录校验之外，不带 token 也应该能访问
        mockMvc.perform(get("/api/hello").param("name", "M0"))  // 发 GET /api/hello?name=M0
                .andExpect(status().isOk())                     // 断言：HTTP 状态码是 200
                .andExpect(jsonPath("$.message").value("Hello, M0!"));  // 断言：JSON 里 message 的值
    }
}