package com.example.firstm0.llm;

import com.example.firstm0.config.LlmProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.function.Consumer;

/**
 * 基于 OpenAI 兼容协议的模型客户端。
 *
 * <p>DeepSeek、通义千问、Kimi、本地 Ollama 都提供“OpenAI 兼容接口”，
 * 也就是同一个请求格式：
 * <pre>
 * POST {baseUrl}/v1/chat/completions
 * Authorization: Bearer {apiKey}
 * { "model": "...", "messages": [ {"role":"system","content":"..."}, {"role":"user","content":"..."} ], "temperature": 0.7 }
 * </pre>
 * 响应里取 {@code choices[0].message.content} 就是模型回复。
 *
 * <p>所以只要配置换一下 baseUrl / model，就能换厂商，代码不用改。
 */
@Component
public class OpenAiCompatibleClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(OpenAiCompatibleClient.class);

    /** OpenAI 兼容接口路径；baseUrl 只配域名，不要带 /v1。 */
    private static final String CHAT_PATH = "/v1/chat/completions";

    /** 连接超时固定 10 秒：连不上要快速失败，不能让用户等 60 秒。 */
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);

    /** 温度越低越稳定；客服场景不适合太发散，用 0.3。 */
    private static final double TEMPERATURE = 0.3;

    private final RestClient restClient;
    private final String model;
    private final String apiKey;

    /** 用来解析流式返回的每一小块 JSON。 */
    private final ObjectMapper objectMapper;

    /**
     * 构造器注入 RestClient.Builder（Spring Boot 自动提供），
     * 好处是测试时可以用 MockRestServiceServer 接管 HTTP，不用真的发请求。
     */
    public OpenAiCompatibleClient(LlmProperties properties, RestClient.Builder builder, ObjectMapper objectMapper) {
        this.model = properties.model();
        this.apiKey = properties.apiKey();
        this.objectMapper = objectMapper;

        // SimpleClientHttpRequestFactory 是最基础的 JDK HTTP 实现，
        // 这里只需要设置两个超时，没必要引入更复杂的连接池。
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(CONNECT_TIMEOUT);
        requestFactory.setReadTimeout(Duration.ofSeconds(properties.timeoutSeconds()));

        this.restClient = builder
                .baseUrl(properties.baseUrl())
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    public String chat(List<LlmMessage> messages) {
        // 没配 Key 时直接给出明确错误，而不是发一个注定 401 的请求
        if (apiKey == null || apiKey.isBlank()) {
            throw new LlmException("未配置 LLM_API_KEY，请在环境变量里设置后再试");
        }

        ChatCompletionRequest request = new ChatCompletionRequest(
                model,
                toChatMessages(messages),
                TEMPERATURE,
                false
        );

        long start = System.currentTimeMillis();
        try {
            ChatCompletionResponse response = restClient.post()
                    .uri(CHAT_PATH)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(ChatCompletionResponse.class);

            String content = extractContent(response);

            log.info("模型调用成功，model={}, 耗时={}ms, 回复长度={}",
                    model, System.currentTimeMillis() - start, content.length());

            return content;

        } catch (RestClientException exception) {
            // 只记录异常类型和状态码，不记录请求头（里面有 Key）和完整响应体
            log.warn("模型调用失败，model={}, 耗时={}ms, 原因={}",
                    model, System.currentTimeMillis() - start, exception.getMessage());

            throw new LlmException("模型调用失败：" + exception.getClass().getSimpleName(), exception);
        }
    }

    @Override
    public String chatStream(List<LlmMessage> messages, Consumer<String> onDelta) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new LlmException("未配置 LLM_API_KEY，请在环境变量里设置后再试");
        }

        ChatCompletionRequest request = new ChatCompletionRequest(
                model,
                toChatMessages(messages),
                TEMPERATURE,
                true   // 关键：告诉服务端“分块返回”
        );

        StringBuilder fullReply = new StringBuilder();
        long start = System.currentTimeMillis();

        try {
            // exchange 能拿到原始响应流；retrieve().body() 会把整个响应读完才返回，做不到流式
            restClient.post()
                    .uri(CHAT_PATH)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.TEXT_EVENT_STREAM)
                    .body(request)
                    .exchange((clientRequest, clientResponse) -> {
                        if (!clientResponse.getStatusCode().is2xxSuccessful()) {
                            throw new LlmException("模型调用失败：HTTP " + clientResponse.getStatusCode().value());
                        }

                        readStream(clientResponse.getBody(), fullReply, onDelta);

                        return null;
                    });

            log.info("模型流式调用完成，model={}, 耗时={}ms, 回复长度={}",
                    model, System.currentTimeMillis() - start, fullReply.length());

        } catch (RestClientException exception) {
            log.warn("模型流式调用失败，model={}, 耗时={}ms, 原因={}",
                    model, System.currentTimeMillis() - start, exception.getMessage());

            throw new LlmException("模型调用失败：" + exception.getClass().getSimpleName(), exception);

        } catch (LlmException exception) {
            log.warn("模型流式调用失败，model={}, 耗时={}ms, 原因={}",
                    model, System.currentTimeMillis() - start, exception.getMessage());

            throw exception;
        }

        String reply = fullReply.toString().strip();

        if (reply.isEmpty()) {
            throw new LlmException("模型返回了空回复");
        }

        return reply;
    }

    /**
     * 逐行读取 SSE 流。
     *
     * <p>DeepSeek / OpenAI 的流式格式长这样，每块一行：
     * <pre>
     * data: {"choices":[{"delta":{"content":"你"}}]}
     * data: {"choices":[{"delta":{"content":"好"}}]}
     * data: [DONE]
     * </pre>
     * 只要以 {@code data:} 开头的行才有用，{@code [DONE]} 表示结束。
     */
    private void readStream(java.io.InputStream body,
                            StringBuilder fullReply,
                            Consumer<String> onDelta) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(body, StandardCharsets.UTF_8))) {

            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.startsWith("data:")) {
                    continue;
                }

                String payload = line.substring("data:".length()).trim();

                if (payload.isEmpty() || "[DONE]".equals(payload)) {
                    continue;
                }

                String delta = extractDelta(payload);

                if (delta != null && !delta.isEmpty()) {
                    fullReply.append(delta);
                    onDelta.accept(delta);   // 立刻推给前端
                }
            }
        } catch (java.io.IOException exception) {
            throw new LlmException("读取模型流式响应失败", exception);
        }
    }

    /** 从一块流式 JSON 里取出文本增量；没有内容（比如只有角色信息）就返回 null。 */
    private String extractDelta(String payload) {
        try {
            StreamChunk chunk = objectMapper.readValue(payload, StreamChunk.class);

            if (chunk.choices() == null || chunk.choices().isEmpty()) {
                return null;
            }

            StreamDelta delta = chunk.choices().get(0).delta();

            return delta == null ? null : delta.content();

        } catch (JsonProcessingException exception) {
            // 单块解析失败不影响整段对话，跳过即可
            log.debug("跳过无法解析的流式分片：{}", exception.getMessage());

            return null;
        }
    }

    /** 把内部消息类型转成请求体需要的结构。 */
    private List<ChatMessage> toChatMessages(List<LlmMessage> messages) {
        return messages.stream()
                .map(message -> new ChatMessage(message.role(), message.content()))
                .toList();
    }

    /** 从响应里取出第一段回复，并挡掉“响应正常但内容为空”的情况。 */
    private String extractContent(ChatCompletionResponse response) {
        if (response == null || response.choices() == null || response.choices().isEmpty()) {
            throw new LlmException("模型没有返回任何结果");
        }

        ChatMessage message = response.choices().get(0).message();
        String content = (message == null) ? null : message.content();

        if (content == null || content.isBlank()) {
            throw new LlmException("模型返回了空回复");
        }

        return content.strip();
    }

    // ---------------------------------------------------------------------
    // 下面几个 record 只用于 JSON 序列化/反序列化，属于实现细节，所以是 private。
    // ---------------------------------------------------------------------

    /** 请求体；stream 为 true 时服务端会分块返回。 */
    private record ChatCompletionRequest(String model, List<ChatMessage> messages,
                                         double temperature, boolean stream) {
    }

    /** 一条消息：role 是 system / user / assistant，content 是内容。 */
    private record ChatMessage(String role, String content) {
    }

    /** 非流式响应体：只声明我们关心的字段，其余字段 Jackson 会自动忽略。 */
    private record ChatCompletionResponse(List<Choice> choices) {
    }

    private record Choice(ChatMessage message) {
    }

    /** 流式响应的一小块。 */
    private record StreamChunk(List<StreamChoice> choices) {
    }

    private record StreamChoice(StreamDelta delta) {
    }

    private record StreamDelta(String content) {
    }
}
