package com.example.firstm0.service;

import com.example.firstm0.common.ConversationNotFoundException;
import com.example.firstm0.domain.Conversation;
import com.example.firstm0.domain.Message;
import com.example.firstm0.domain.MessageRole;
import com.example.firstm0.dto.ChatResponse;
import com.example.firstm0.dto.MessageResponse;
import com.example.firstm0.llm.LlmClient;
import com.example.firstm0.llm.LlmException;
import com.example.firstm0.llm.LlmMessage;
import com.example.firstm0.repository.ConversationRepository;
import com.example.firstm0.repository.MessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 聊天业务逻辑。
 *
 * <p>M0.3 起，回复不再固定，而是调用真实大模型。
 *
 * <p>关键设计：<b>把等模型的时间放在事务外面</b>。
 * 一次模型调用可能几秒到几十秒，如果包在一个大事务里，
 * 数据库连接会被一直占着，并发一高连接池就耗尽。
 * 所以拆成三段：
 * <ol>
 *   <li>事务 1：建会话 + 保存用户消息，立即提交；</li>
 *   <li>事务外：调用大模型（这一步不持有数据库事务）；</li>
 *   <li>事务 2：保存机器人回复。</li>
 * </ol>
 *
 * <p>为什么用 {@link TransactionTemplate} 而不是在方法上加 {@code @Transactional}？
 * 因为在同一个类里自己调自己的方法，{@code @Transactional} 是不生效的
 * （Spring 的事务是代理实现的，自调用不会经过代理）。
 * 用 TransactionTemplate 显式写明事务边界，既正确又直观。
 */
@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    /** 会话标题最长 100，和数据库字段长度保持一致。 */
    private static final int MAX_TITLE_LENGTH = 100;

    /**
     * 历史总字符预算。
     *
     * <p>为什么要限制：上下文越长，模型调用越慢、越贵，
     * 而且超过模型的上下文窗口后接口会直接报错。
     * 4000 字符对客服场景够用，也能把成本控制住。
     */
    private static final int MAX_HISTORY_CHARS = 4000;

    /**
     * 系统提示词：规定模型的角色和边界。
     * M0.4 起和历史消息一起发给模型，M2 会再加上知识库资料。
     */
    private static final String SYSTEM_PROMPT = """
            你是「FirstM0」的客服助手。
            回答要简洁、礼貌，只基于已知信息回答。
            不知道答案时，明确说“这个问题我需要帮你转人工确认”，不要编造。
            不要编造订单、物流、退款、价格信息。
            """;

    /** 模型不可用时的兜底回复，保证接口永远有内容返回，不把错误甩给用户。 */
    private static final String FALLBACK_REPLY =
            "抱歉，客服系统现在有点忙，请稍后再试。如果比较着急，建议联系人工客服。";

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final LlmClient llmClient;
    private final TransactionTemplate transactionTemplate;

    public ChatService(ConversationRepository conversationRepository,
                       MessageRepository messageRepository,
                       LlmClient llmClient,
                       PlatformTransactionManager transactionManager) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.llmClient = llmClient;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    /**
     * 处理一次用户发消息。
     *
     * @param conversationId 会话 id，第一次聊天传 null
     * @param userMessage    用户消息（已由 Controller 校验）
     * @return 会话 id + 机器人回复
     */
    public ChatResponse chat(Long conversationId, String userMessage) {
        // 第 1 段：事务内，建会话 + 存用户消息，提交后立刻释放连接
        Long savedConversationId = Objects.requireNonNull(
                transactionTemplate.execute(status -> saveUserTurn(conversationId, userMessage)));

        // 第 2 段：事务外，组装上下文并等模型返回；可能很慢，但不占用数据库连接
        String reply = generateReply(buildPrompt(savedConversationId));

        // 第 3 段：事务内，存机器人回复并刷新会话更新时间
        transactionTemplate.executeWithoutResult(status -> saveAssistantTurn(savedConversationId, reply));

        return new ChatResponse(savedConversationId, reply);
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

    /** 第一段事务：查/建会话，并存下用户这条消息。 */
    private Long saveUserTurn(Long conversationId, String userMessage) {
        LocalDateTime now = LocalDateTime.now();

        Conversation conversation = (conversationId == null)
                ? conversationRepository.save(new Conversation(titleFrom(userMessage), now))
                : conversationRepository.findById(conversationId)
                        .orElseThrow(() -> new ConversationNotFoundException(conversationId));

        messageRepository.save(new Message(conversation.getId(), MessageRole.USER, userMessage, now));
        conversation.touch(now);

        return conversation.getId();
    }

    /** 第三段事务：存下机器人这条回复，同时刷新会话的最后更新时间。 */
    private void saveAssistantTurn(Long conversationId, String reply) {
        LocalDateTime now = LocalDateTime.now();

        messageRepository.save(new Message(conversationId, MessageRole.ASSISTANT, reply, now));
        conversationRepository.findById(conversationId).ifPresent(conversation -> conversation.touch(now));
    }

    /**
     * 组装发给模型的消息列表：system prompt + 历史对话。
     *
     * <p>当前这条用户消息已经在上一步存进数据库了，
     * 所以查最近 10 条时它就在里面，不需要再单独追加。
     *
     * <p>裁剪规则（M0.4 的核心）：
     * <ol>
     *   <li>先取最近 10 条（由 Repository 的 findTop10 保证）；</li>
     *   <li>从最新往旧累加字符，总数超过 4000 就不再往前取；</li>
     *   <li>最新的一条无论多长都要保留，保证模型至少看到当前问题；</li>
     *   <li>最后翻回时间正序——模型需要按“先说 → 后说”的顺序读。</li>
     * </ol>
     * 这样上下文长度有上限，token 成本不会随对话轮数无限增长。
     */
    private List<LlmMessage> buildPrompt(Long conversationId) {
        // 查出来是“最新在前”，先复制一份可变列表，避免直接改数据库返回的集合
        List<Message> recent = new ArrayList<>(
                messageRepository.findTop10ByConversationIdOrderByIdDesc(conversationId));

        List<LlmMessage> history = new ArrayList<>();
        int totalChars = 0;

        for (int i = 0; i < recent.size(); i++) {
            Message message = recent.get(i);
            int length = message.getContent().length();

            // history 非空说明最新那条已经装进去了，这时候才允许因为超预算而停止
            if (!history.isEmpty() && totalChars + length > MAX_HISTORY_CHARS) {
                break;
            }

            history.add(toLlmMessage(message));
            totalChars += length;
        }

        // 上面是“新 → 旧”，翻成“旧 → 新”再发给模型
        Collections.reverse(history);

        List<LlmMessage> messages = new ArrayList<>(history.size() + 1);
        messages.add(LlmMessage.system(SYSTEM_PROMPT));
        messages.addAll(history);

        log.debug("组装上下文：共 {} 条消息，历史字符数 {}", messages.size(), totalChars);

        return messages;
    }

    /** 把数据库实体转成模型消息；角色只有两种，直接映射。 */
    private LlmMessage toLlmMessage(Message message) {
        return message.getRole() == MessageRole.USER
                ? LlmMessage.user(message.getContent())
                : LlmMessage.assistant(message.getContent());
    }

    /**
     * 调用大模型生成回复。
     *
     * <p>失败不抛给前端：记一条 WARN 日志，返回兜底话术。
     * 这样用户至少能看到“系统忙”，而不是一个 500 页面。
     */
    private String generateReply(List<LlmMessage> messages) {
        try {
            return llmClient.chat(messages);
        } catch (LlmException exception) {
            log.warn("大模型调用失败，改用兜底回复: {}", exception.getMessage());

            return FALLBACK_REPLY;
        }
    }

    /** 用第一句话当标题，超长时截断，避免超过数据库字段长度。 */
    private String titleFrom(String userMessage) {
        String trimmed = userMessage.strip();

        return trimmed.length() <= MAX_TITLE_LENGTH
                ? trimmed
                : trimmed.substring(0, MAX_TITLE_LENGTH);
    }
}
