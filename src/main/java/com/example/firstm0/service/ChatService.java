package com.example.firstm0.service;

import com.example.firstm0.common.ConversationNotFoundException;
import com.example.firstm0.domain.Conversation;
import com.example.firstm0.domain.Message;
import com.example.firstm0.domain.MessageRole;
import com.example.firstm0.dto.ChatResponse;
import com.example.firstm0.dto.MessageResponse;
import com.example.firstm0.repository.ConversationRepository;
import com.example.firstm0.repository.MessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 聊天业务逻辑。
 *
 * <p>Controller 只负责接请求、返响应，真正的业务都放在这里：
 * 新建会话、保存消息、生成回复（M0.3 会换成真实大模型调用）、查历史。
 *
 * <p>M0.2 的流程：
 * <ol>
 *   <li>conversationId 为空 → 新建一个会话；不为空 → 查出已有会话（查不到抛 404）</li>
 *   <li>保存 user 消息</li>
 *   <li>生成回复（暂时还是固定格式）</li>
 *   <li>保存 assistant 消息</li>
 *   <li>返回 conversationId + reply</li>
 * </ol>
 */
@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    /** 会话标题最长 100，和数据库字段长度保持一致。 */
    private static final int MAX_TITLE_LENGTH = 100;

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;

    /** 构造器注入：Spring 启动时把两个 Repository 传进来。 */
    public ChatService(ConversationRepository conversationRepository,
                       MessageRepository messageRepository) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
    }

    /**
     * 处理一次用户发消息。
     *
     * <p>{@code @Transactional} 表示这个方法里的数据库操作在同一个事务里：
     * 要么全部成功，要么全部回滚，不会出现“用户消息存了、回复没存”的半截数据。
     *
     * <p>注意：M0.3 接大模型后，“等模型返回”不能放在这个大事务里，
     * 否则数据库连接会被长时间占用，需要拆成“保存用户消息 → 调模型 → 保存回复”三段。
     *
     * @param conversationId 会话 id，第一次聊天传 null
     * @param userMessage    用户消息（已由 Controller 校验）
     * @return 会话 id + 机器人回复
     */
    @Transactional
    public ChatResponse chat(Long conversationId, String userMessage) {
        LocalDateTime now = LocalDateTime.now();

        // 1. 没有会话就新建；有会话就查出来，查不到说明前端传了个不存在的 id
        Conversation conversation = findOrCreateConversation(conversationId, userMessage, now);

        // 2. 先保存用户消息（即使后面生成回复失败，也能留下用户说过的话）
        messageRepository.save(new Message(conversation.getId(), MessageRole.USER, userMessage, now));

        // 3. 生成回复：M0.2 还是固定格式，M0.3 换成调用大模型
        String reply = generateReply(userMessage);

        // 4. 保存机器人回复
        messageRepository.save(new Message(conversation.getId(), MessageRole.ASSISTANT, reply, LocalDateTime.now()));

        // 5. 刷新会话的最后更新时间（实体在事务里，JPA 会自动更新这一行）
        conversation.touch(LocalDateTime.now());

        log.info("会话 {} 新增一轮对话，用户消息长度={}", conversation.getId(), userMessage.length());

        return new ChatResponse(conversation.getId(), reply);
    }

    /**
     * 查询某个会话的全部历史消息，按时间从早到晚排列。
     *
     * @param conversationId 会话 id
     * @return 消息列表（没有消息时是空列表）
     * @throws ConversationNotFoundException 会话不存在时抛出，最终变成 404
     */
    public List<MessageResponse> listMessages(Long conversationId) {
        // 先确认会话存在，否则返回空列表会让人误以为“会话存在只是没消息”
        if (!conversationRepository.existsById(conversationId)) {
            throw new ConversationNotFoundException(conversationId);
        }

        return messageRepository.findByConversationIdOrderByIdAsc(conversationId).stream()
                .map(MessageResponse::from)
                .toList();
    }

    private Conversation findOrCreateConversation(Long conversationId, String userMessage, LocalDateTime now) {
        if (conversationId == null) {
            // 第一次聊天：用用户第一句话当标题
            return conversationRepository.save(new Conversation(titleFrom(userMessage), now));
        }

        return conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ConversationNotFoundException(conversationId));
    }

    /** 用第一句话当标题，超长时截断，避免超过数据库字段长度。 */
    private String titleFrom(String userMessage) {
        String trimmed = userMessage.strip();
        return trimmed.length() <= MAX_TITLE_LENGTH
                ? trimmed
                : trimmed.substring(0, MAX_TITLE_LENGTH);
    }

    /**
     * 生成回复。
     *
     * <p>M0.2 是固定格式的假回复；M0.3 会把这里替换成调用大模型。
     */
    private String generateReply(String userMessage) {
        return "我收到了：" + userMessage;
    }
}
