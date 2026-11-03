package com.example.firstm0.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * 会话实体，对应数据库表 conversation。
 *
 * <p>实体（Entity）和 DTO 的区别：
 * <ul>
 *   <li>实体：描述“数据库里的一行数据”，归 JPA 管，字段和表结构一一对应。</li>
 *   <li>DTO：描述“接口的请求/响应格式”，归 Controller 管，想怎么改就怎么改。</li>
 * </ul>
 * 不要把实体直接返回给前端，否则数据库结构一改接口就跟着变。
 *
 * <p>一个会话（Conversation）下面有很多条消息（Message）。
 * M0.2 先用 conversation_id 字段关联，不写 @ManyToOne，
 * 避免一上来就遇到懒加载（Lazy Loading）这类复杂问题。
 */
@Entity
@Table(name = "conversation")
public class Conversation {

    /** 主键。IDENTITY 表示交给数据库自增（MySQL 的 AUTO_INCREMENT）。 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 会话标题，先用用户第一句话截断生成，最长 100 字符。 */
    @Column(nullable = false, length = 100)
    private String title;

    /** 创建时间。Java 字段 camelCase，数据库列名用 @Column 指定成 snake_case。 */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /** 最后更新时间，每次有新消息时刷新，方便以后按活跃时间排序会话列表。 */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * JPA 规范要求实体必须有一个无参构造器，它靠反射创建对象。
     * 设为 protected，既能满足 JPA，又不让业务代码随便 new 一个空对象。
     */
    protected Conversation() {
    }

    /**
     * 业务使用的构造器：新建会话时调用。
     *
     * @param title 会话标题
     * @param now   当前时间，由 Service 传入，方便测试时控制时间
     */
    public Conversation(String title, LocalDateTime now) {
        this.title = title;
        this.createdAt = now;
        this.updatedAt = now;
    }

    /** 有新消息时更新“最后更新时间”。 */
    public void touch(LocalDateTime now) {
        this.updatedAt = now;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
