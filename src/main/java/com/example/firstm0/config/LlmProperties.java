package com.example.firstm0.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 大模型相关配置，对应 application.yml 里的 {@code llm.*}。
 *
 * <p>用 record 的原因：配置一旦在启动时读入就不该再改，record 天然不可变，最合适。
 *
 * <p>Spring Boot 会把 yml 里的
 * {@code llm.base-url} → {@code baseUrl}（中划线转驼峰），
 * 自动传进这个构造器。需要在启动类上开启 {@code @ConfigurationPropertiesScan}。
 *
 * @param baseUrl        模型服务的根地址，例如 https://api.deepseek.com
 * @param apiKey         API Key，只从环境变量 LLM_API_KEY 读取，绝不写进代码
 * @param model          模型名，例如 deepseek-chat
 * @param timeoutSeconds 读取超时时间（秒），连接超时在客户端里固定为 10 秒
 */
@ConfigurationProperties(prefix = "llm")
public record LlmProperties(
        String baseUrl,
        String apiKey,
        String model,
        int timeoutSeconds
) {

    /**
     * 紧凑构造器：给 baseUrl / model / 超时兜底默认值。
     *
     * <p>为什么需要兜底：测试环境的 application.yml 不写 llm 段，
     * 这几个字段会是 null / 0；如果不给默认值，
     * 创建 HTTP 客户端时就可能出错。apiKey 故意不给默认值，
     * 它就是“有没有配好”的判断依据。
     */
    public LlmProperties {
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "https://api.deepseek.com";
        }
        if (model == null || model.isBlank()) {
            model = "deepseek-chat";
        }
        if (timeoutSeconds <= 0) {
            timeoutSeconds = 60;
        }
    }
}
