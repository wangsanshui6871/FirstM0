package com.example.firstm0.controller;

import com.example.firstm0.auth.AuthAttributes;
import com.example.firstm0.dto.ConversationSummary;
import com.example.firstm0.dto.MessageResponse;
import com.example.firstm0.dto.RenameConversationRequest;
import com.example.firstm0.service.ChatService;
import com.example.firstm0.service.ConversationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 会话相关接口。
 *
 * <p>所有接口都要求登录（由 AuthInterceptor 保证），
 * 并且只能操作自己的会话——别人的会话一律返回 404。
 */
@RestController
@RequestMapping("/api/conversations")
public class ConversationController {

    private final ChatService chatService;
    private final ConversationService conversationService;

    public ConversationController(ChatService chatService, ConversationService conversationService) {
        this.chatService = chatService;
        this.conversationService = conversationService;
    }

    /** 我的会话列表，最近更新的在前。 */
    @GetMapping
    public List<ConversationSummary> list(@RequestAttribute(AuthAttributes.USER_ID) Long userId) {
        return conversationService.list(userId);
    }

    /**
     * 处理 {@code GET /api/conversations/{id}/messages}。
     *
     * <p>@PathVariable 把 URL 里的 {id} 绑定到参数上。
     * 会话不存在或不属于当前用户时，Service 抛 ConversationNotFoundException，最终返回 404。
     */
    @GetMapping("/{id}/messages")
    public List<MessageResponse> messages(@RequestAttribute(AuthAttributes.USER_ID) Long userId,
                                          @PathVariable Long id) {
        return chatService.listMessages(userId, id);
    }

    /** 重命名。 */
    @PatchMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void rename(@RequestAttribute(AuthAttributes.USER_ID) Long userId,
                       @PathVariable Long id,
                       @Valid @RequestBody RenameConversationRequest request) {
        conversationService.rename(userId, id, request.title());
    }

    /** 删除会话及其消息。 */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@RequestAttribute(AuthAttributes.USER_ID) Long userId,
                       @PathVariable Long id) {
        conversationService.delete(userId, id);
    }
}
