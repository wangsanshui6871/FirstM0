package com.example.firstm0.controller;

import com.example.firstm0.auth.AuthAttributes;
import com.example.firstm0.dto.ChatRequest;
import com.example.firstm0.dto.ChatResponse;
import com.example.firstm0.service.ChatService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 聊天接口。
 *
 * <p>本类只做三件事：
 * <ol>
 *   <li>接收 POST 请求，把 JSON 请求体反序列化成 ChatRequest；</li>
 *   <li>用 @Valid 触发参数校验（不合法时由 GlobalExceptionHandler 统一返回 400）；</li>
 *   <li>调用 ChatService 拿到回复，包装成 ChatResponse 返回。</li>
 * </ol>
 * <p>业务细节一律不写在这里。
 *
 * <p>M1.2 起多了一个 userId 参数：它由 AuthInterceptor 校验 token 后放进 request，
 * 这里用 {@code @RequestAttribute} 取出来。没登录的请求根本到不了这个方法。
 */
@RestController
@RequestMapping("/api")
public class ChatController {

    private final ChatService chatService;

    /**
     * 构造器注入：Spring 启动时会自动把 ChatService 传进来。
     * 项目规范要求用构造器注入，而不是在字段上写 @Autowired。
     */
    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    /**
     * 处理 {@code POST /api/chat}。
     *
     * @param userId  当前登录用户 id，由拦截器写入
     * @param request 请求体；@RequestBody 负责把 JSON 转成 record，
     *                @Valid 负责按 record 上的注解做校验
     * @return 含 conversationId 和 reply 的响应
     */
    @PostMapping("/chat")
    public ChatResponse chat(@RequestAttribute(AuthAttributes.USER_ID) Long userId,
                             @Valid @RequestBody ChatRequest request) {
        // 交给 Service 处理：建会话、存两条消息、生成回复，返回真实 conversationId
        return chatService.chat(userId, request.conversationId(), request.message());
    }
}
