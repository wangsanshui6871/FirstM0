package com.example.firstm0;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 启动冒烟测试：只验证 Spring 上下文能否正常加载。
 *
 * <p>@SpringBootTest 会启动完整上下文（比 @WebMvcTest 重）。
 * 如果这个方法挂了，说明配置 / 依赖 / Bean 装配有问题，要优先修它，再看别的失败。
 */
@SpringBootTest
class FirstM0ApplicationTests {

    @Test
    void contextLoads() {
        // 方法体是空的：只要上下文能成功加载，这个测试就算通过
    }
}