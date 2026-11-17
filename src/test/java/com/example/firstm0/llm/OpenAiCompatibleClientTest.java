package com.example.firstm0.llm;

import com.example.firstm0.config.LlmProperties;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * OpenAiCompatibleClient 的 HTTP 层测试。
 *
 * <p>用 JDK 自带的 HttpServer 在 127.0.0.1 上起一个极简假服务器，
 * 这样能真实走一遍“发请求 → 收响应 → 解析 JSON”，
 * 又不需要外部网络、不需要真实 API Key、不会花钱。
 *
 * <p>每个测试自己决定假服务器返回什么（状态码 + 响应体），
 * 从而覆盖正常、空回复、服务端报错三种情况。
 */
class OpenAiCompatibleClientTest {

    private static final String SYSTEM_PROMPT = "你是客服助手";
    private static final String USER_MESSAGE = "你好";

    /** 每次请求都用同一组消息，省得每个用例重复写。 */
    private static final List<LlmMessage> MESSAGES = List.of(
            LlmMessage.system(SYSTEM_PROMPT),
            LlmMessage.user(USER_MESSAGE)
    );

    private HttpServer server;
    private String baseUrl;

    /** 假服务器要返回的状态码和响应体，测试开始前设置。 */
    private volatile int statusCode;
    private volatile String responseBody;

    @BeforeEach
    void startFakeServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/chat/completions", exchange -> {
            // 必须把请求体读完，否则客户端可能收到“连接被重置”
            exchange.getRequestBody().readAllBytes();

            byte[] bytes = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(statusCode, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();

        // 端口传 0 让系统随机分配，避免和本机其他程序冲突
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @AfterEach
    void stopFakeServer() {
        server.stop(0);
    }

    /**
     * 正常响应：应返回 choices[0].message.content，并去掉首尾空白。
     */
    @Test
    void returnsContentFromFirstChoice() {
        statusCode = 200;
        responseBody = """
                {
                  "id": "chat-1",
                  "choices": [
                    { "index": 0, "message": { "role": "assistant", "content": "  你好，我是客服助手  " } }
                  ],
                  "usage": { "prompt_tokens": 10, "completion_tokens": 8 }
                }
                """;

        LlmClient client = newClient("test-key");

        assertThat(client.chat(MESSAGES)).isEqualTo("你好，我是客服助手");
    }

    /**
     * 响应结构正常但 content 是空白：应抛 LlmException，而不是返回空字符串。
     */
    @Test
    void throwsWhenContentIsBlank() {
        statusCode = 200;
        responseBody = """
                { "choices": [ { "message": { "role": "assistant", "content": "   " } } ] }
                """;

        LlmClient client = newClient("test-key");

        assertThatThrownBy(() -> client.chat(MESSAGES))
                .isInstanceOf(LlmException.class)
                .hasMessageContaining("空回复");
    }

    /**
     * 服务端返回 500：应抛 LlmException，而不是把原始异常直接抛给上层。
     */
    @Test
    void throwsWhenServerReturnsError() {
        statusCode = 500;
        responseBody = """
                { "error": { "message": "internal error" } }
                """;

        LlmClient client = newClient("test-key");

        assertThatThrownBy(() -> client.chat(MESSAGES))
                .isInstanceOf(LlmException.class)
                .hasMessageContaining("模型调用失败");
    }

    /**
     * 没有配置 API Key：应在本地就直接报错，根本不发请求。
     */
    @Test
    void throwsWhenApiKeyMissing() {
        statusCode = 200;
        responseBody = "{}";

        LlmClient client = newClient("");

        assertThatThrownBy(() -> client.chat(MESSAGES))
                .isInstanceOf(LlmException.class)
                .hasMessageContaining("LLM_API_KEY");
    }

    /** 用假服务器地址构造一个客户端。 */
    private LlmClient newClient(String apiKey) {
        LlmProperties properties = new LlmProperties(baseUrl, apiKey, "deepseek-chat", 5);

        return new OpenAiCompatibleClient(properties, RestClient.builder());
    }
}
