package com.example.firstm0.repository;

import com.example.firstm0.domain.Message;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 消息表的数据库操作接口。
 *
 * <p>findByConversationIdOrderByIdAsc 是“方法名即查询”的写法：
 * Spring Data JPA 会解析方法名，自动生成
 * {@code select ... from message where conversation_id = ? order by id asc}。
 * 不需要手写 SQL。
 */
public interface MessageRepository extends JpaRepository<Message, Long> {

    /**
     * 按会话 id 查消息，并按 id 升序（相当于按发送时间从早到晚）。
     *
     * @param conversationId 会话 id
     * @return 该会话的消息列表，没有消息时返回空列表（不是 null）
     */
    List<Message> findByConversationIdOrderByIdAsc(Long conversationId);
}
