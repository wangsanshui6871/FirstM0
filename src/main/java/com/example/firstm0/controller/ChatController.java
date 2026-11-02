package com.example.firstm0.controller;

import com.example.firstm0.dto.ChatRequest;
import com.example.firstm0.dto.ChatResponse;
import com.example.firstm0.service.ChatService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
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
     * @param request 请求体；@RequestBody 负责把 JSON 转成 record，
     *                @Valid 负责按 record 上的注解做校验
     * @return 含 conversationId 和 reply 的响应
     */
    @PostMapping("/chat")
    public ChatResponse chat(@Valid @RequestBody ChatRequest request) {
        // 交给 Service 处理业务，Controller 不关心它怎么生成回复
        String reply = chatService.generateReply(request.message());

        // M0.1 直接把请求里的 conversationId 原样返回；M0.2 会换成数据库生成的真实 id
        return new ChatResponse(request.conversationId(), reply);
    }
}
