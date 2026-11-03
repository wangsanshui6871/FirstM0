package com.example.firstm0.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * 消息实体，对应数据库表 message。
 *
 * <p>每条消息属于一个会话，用 conversation_id 关联。
 * M0.2 故意只存 Long conversationId，不用 @ManyToOne，
 * 这样查询历史时不需要处理懒加载，代码更好理解。
 */
@Entity
@Table(name = "message", indexes = {
        // 复合索引：查某个会话的消息时，条件是 conversation_id，排序用 id。
        // 有了这个索引，数据库不用全表扫描，会话消息越多优势越明显。
        @Index(name = "idx_message_conversation", columnList = "conversation_id, id")
})
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 所属会话 id，数据库列名 conversation_id。 */
    @Column(name = "conversation_id", nullable = false)
    private Long conversationId;

    /**
     * 谁说的：USER 或 ASSISTANT。
     * EnumType.STRING 让数据库存字符串而不是序号；
     * columnDefinition = "varchar(20)" 明确用 VARCHAR，而不是 MySQL 的 ENUM 类型，
     * 以后想加新的角色只要改代码，不用改表结构。
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "varchar(20)")
    private MessageRole role;

    /** 消息正文。columnDefinition = "TEXT" 表示用 MySQL 的 TEXT 类型，能存长文本。 */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /** JPA 要求的无参构造器。 */
    protected Message() {
    }

    /**
     * 业务使用的构造器：保存一条新消息时调用。
     *
     * @param conversationId 所属会话
     * @param role           谁说的
     * @param content        内容
     * @param now            当前时间
     */
    public Message(Long conversationId, MessageRole role, String content, LocalDateTime now) {
        this.conversationId = conversationId;
        this.role = role;
        this.content = content;
        this.createdAt = now;
    }

    public Long getId() {
        return id;
    }

    public Long getConversationId() {
        return conversationId;
    }

    public MessageRole getRole() {
        return role;
    }

    public String getContent() {
        return content;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
