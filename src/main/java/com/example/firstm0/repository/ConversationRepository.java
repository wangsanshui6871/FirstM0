package com.example.firstm0.repository;

import com.example.firstm0.domain.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 会话表的数据库操作接口。
 *
 * <p>只要继承 JpaRepository，Spring Data JPA 就会在启动时自动生成实现类，
 * 我们不用写 SQL，也不用写实现类，就能直接用 save / findById / findAll 等方法。
 *
 * @param <Conversation> 操作的实体类型
 * @param <Long>         主键类型
 */
public interface ConversationRepository extends JpaRepository<Conversation, Long> {
}
