package com.example.firstm0.controller;

import com.example.firstm0.dto.HelloResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Hello 接口 —— M0 阶段用来验证「浏览器 → 后端 → JSON」这条链路是否打通。
 *
 * <p>Controller 层的职责只有三件事：
 * <ol>
 *   <li>接收 HTTP 请求，把请求参数绑定到方法入参；</li>
 *   <li>（M0.1 起）校验参数是否合法；</li>
 *   <li>把返回对象交出去，Spring 会自动序列化成 JSON。</li>
 * </ol>
 * <p>真正的业务逻辑不写在这里，以后放进 service 层。
 */
@RestController                 // = @Controller + @ResponseBody：方法返回值直接作为 JSON 响应体
@RequestMapping("/api")         // 类级路径前缀：本类所有接口都以 /api 开头
public class HelloController {

    /**
     * 处理 {@code GET /api/hello?name=xxx}。
     *
     * @param name 来自 URL 的查询参数；不传时使用默认值 "Spring Boot"
     * @return 含 message 字段的响应对象，Spring 会把它转成 {"message":"..."}
     */
    @GetMapping("/hello")       // 只响应 GET 请求
    public HelloResponse hello(@RequestParam(defaultValue = "Spring Boot") String name) {
        // 这里就是最朴素的“业务”：拼一段文本。真实项目里这一段会交给 Service 层
        return new HelloResponse("Hello, " + name + "!");
    }
}