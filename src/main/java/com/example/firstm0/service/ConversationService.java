package com.example.firstm0.service;

import com.example.firstm0.common.ConversationNotFoundException;
import com.example.firstm0.domain.Conversation;
import com.example.firstm0.dto.ConversationSummary;
import com.example.firstm0.repository.ConversationRepository;
import com.example.firstm0.repository.MessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 会话的列表、重命名、删除。
 *
 * <p>从 ChatService 里拆出来的原因：
 * 聊天负责“发消息、调模型”，管理负责“列出 / 改名 / 删除”，
 * 两者变化的原因不一样，放一个类里会越长越乱。
 *
 * <p>所有方法都要求传 userId，并且查询时把 userId 带进 where 条件，
 * 所以操作别人的会话会直接查不到，而不是“查到了再判断”。
 */
@Service
public class ConversationService {

    private static final Logger log = LoggerFactory.getLogger(ConversationService.class);

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;

    public ConversationService(ConversationRepository conversationRepository,
                               MessageRepository messageRepository) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
    }

    /** 当前用户的会话列表，最近更新的在前。 */
    @Transactional(readOnly = true)
    public List<ConversationSummary> list(Long userId) {
        return conversationRepository.findByUserIdOrderByUpdatedAtDesc(userId).stream()
                .map(ConversationSummary::from)
                .toList();
    }

    /**
     * 重命名。
     *
     * @throws ConversationNotFoundException 会话不存在，或不属于当前用户
     */
    @Transactional
    public void rename(Long userId, Long conversationId, String newTitle) {
        Conversation conversation = requireOwned(userId, conversationId);
        conversation.rename(newTitle.strip());

        log.info("会话重命名，userId={}, conversationId={}", userId, conversationId);
    }

    /**
     * 删除会话及其消息。
     *
     * <p>先删消息再删会话：没有外键级联，顺序错了会留下孤儿消息。
     */
    @Transactional
    public void delete(Long userId, Long conversationId) {
        Conversation conversation = requireOwned(userId, conversationId);

        messageRepository.deleteByConversationId(conversation.getId());
        conversationRepository.delete(conversation);

        log.info("会话已删除，userId={}, conversationId={}", userId, conversationId);
    }

    /**
     * 查出“属于这个用户的”会话。
     *
     * <p>别人的会话和不存在返回同一个 404，
     * 这样连“这个 id 有没有被人用过”都探测不出来。
     */
    private Conversation requireOwned(Long userId, Long conversationId) {
        return conversationRepository.findByIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new ConversationNotFoundException(conversationId));
    }
}
