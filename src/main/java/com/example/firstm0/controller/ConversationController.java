package com.example.firstm0.controller;

import com.example.firstm0.dto.MessageResponse;
import com.example.firstm0.service.ChatService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 会话相关接口。
 *
 * <p>M0.2 只有一个能力：按会话 id 查历史消息。
 * 会话列表、重命名、删除放到 M1 再做。
 */
@RestController
@RequestMapping("/api/conversations")
public class ConversationController {

    private final ChatService chatService;

    public ConversationController(ChatService chatService) {
        this.chatService = chatService;
    }

    /**
     * 处理 {@code GET /api/conversations/{id}/messages}。
     *
     * <p>@PathVariable 把 URL 里的 {id} 绑定到参数上。
     * 会话不存在时 Service 抛 ConversationNotFoundException，最终返回 404。
     */
    @GetMapping("/{id}/messages")
    public List<MessageResponse> messages(@PathVariable Long id) {
        return chatService.listMessages(id);
    }
}
