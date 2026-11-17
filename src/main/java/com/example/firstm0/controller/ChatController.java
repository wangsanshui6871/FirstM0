package com.example.firstm0.controller;

import com.example.firstm0.auth.AuthAttributes;
import com.example.firstm0.dto.ChatRequest;
import com.example.firstm0.dto.ChatResponse;
import com.example.firstm0.service.ChatService;
import com.example.firstm0.service.ConversationService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;

/**
 * 聊天接口。
 *
 * <p>提供两个版本：
 * <ul>
 *   <li>{@code POST /api/chat}：等模型全部生成完再一次性返回，简单；</li>
 *   <li>{@code POST /api/chat/stream}：SSE 流式，边生成边返回，体验好。</li>
 * </ul>
 *
 * <p>业务细节一律不写在这里。
 */
@RestController
@RequestMapping("/api")
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    /** 流式连接最长保持 3 分钟；超时后浏览器会收到结束信号。 */
    private static final long STREAM_TIMEOUT_MS = 180_000L;

    private final ChatService chatService;
    private final ConversationService conversationService;
    private final AsyncTaskExecutor taskExecutor;

    public ChatController(ChatService chatService,
                          ConversationService conversationService,
                          AsyncTaskExecutor taskExecutor) {
        this.chatService = chatService;
        this.conversationService = conversationService;
        this.taskExecutor = taskExecutor;
    }

    /**
     * 处理 {@code POST /api/chat}（非流式，M0.3 起就有）。
     */
    @PostMapping("/chat")
    public ChatResponse chat(@RequestAttribute(AuthAttributes.USER_ID) Long userId,
                             @Valid @RequestBody ChatRequest request) {
        return chatService.chat(userId, request.conversationId(), request.message());
    }

    /**
     * 处理 {@code POST /api/chat/stream}（SSE 流式，M1.3 新增）。
     *
     * <p>返回的不是普通 JSON，而是 {@code text/event-stream}：
     * 连接建立后不关闭，服务端每生成一小段就推一条事件。
     *
     * <p>事件类型：
     * <ul>
     *   <li>{@code meta}：{@code {"conversationId":23}}，会话一创建就发；</li>
     *   <li>{@code delta}：{@code {"content":"你"}}，一小段文字；</li>
     *   <li>{@code done}：{@code {"conversationId":23,"reply":"完整回复"}}；</li>
     *   <li>{@code error}：{@code {"message":"..."}}。</li>
     * </ul>
     *
     * <p>为什么用 POST 而不是 GET：
     * EventSource 只支持 GET 且不能带自定义请求头，
     * 所以前端改用 fetch 读流，这样能正常带 Authorization。
     */
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@RequestAttribute(AuthAttributes.USER_ID) Long userId,
                                 @Valid @RequestBody ChatRequest request) {

        // 先同步校验会话归属：这样“会话不存在”还能正常返回 404。
        // 一旦开始推流，响应头就已经发出去了，再改状态码来不及。
        if (request.conversationId() != null) {
            conversationService.ensureOwned(userId, request.conversationId());
        }

        SseEmitter emitter = new SseEmitter(STREAM_TIMEOUT_MS);

        // 放到独立线程里跑：Tomcat 的请求线程要立刻归还，
        // 否则一个慢请求会一直占着线程池。
        taskExecutor.execute(() -> streamReply(userId, request, emitter));

        return emitter;
    }

    /** 后台线程里的实际推流过程。 */
    private void streamReply(Long userId, ChatRequest request, SseEmitter emitter) {
        try {
            ChatResponse response = chatService.chatStreaming(
                    userId,
                    request.conversationId(),
                    request.message(),
                    conversationId -> send(emitter, "meta", Map.of("conversationId", conversationId)),
                    delta -> send(emitter, "delta", Map.of("content", delta))
            );

            send(emitter, "done", Map.of(
                    "conversationId", response.conversationId(),
                    "reply", response.reply()
            ));

            emitter.complete();

        } catch (Exception exception) {
            log.warn("流式对话失败: {}", exception.getMessage());

            try {
                send(emitter, "error", Map.of("message", "回复生成失败，请稍后重试"));
            } catch (Exception ignored) {
                // 客户端可能早就断开了，这里不用再管
            }

            emitter.complete();
        }
    }

    /**
     * 发一条 SSE 事件。
     *
     * <p>payload 用 JSON 发：文字里如果有换行，直接当纯文本塞进 SSE 会破坏格式，
     * 序列化成 JSON 后就只剩 {@code \n} 转义，安全得多。
     */
    private void send(SseEmitter emitter, String eventName, Object payload) {
        try {
            emitter.send(SseEmitter.event()
                    .name(eventName)
                    .data(payload, MediaType.APPLICATION_JSON));
        } catch (IOException exception) {
            // 包成非受检异常，让它一路传到 Service，从而中断流式读取
            throw new UncheckedIOException(exception);
        }
    }
}
