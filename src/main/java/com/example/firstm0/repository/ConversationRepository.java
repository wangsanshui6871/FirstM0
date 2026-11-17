package com.example.firstm0.repository;

import com.example.firstm0.domain.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 会话表的数据库操作接口。
 *
 * <p>只要继承 JpaRepository，Spring Data JPA 就会在启动时自动生成实现类，
 * 我们不用写 SQL，也不用写实现类，就能直接用 save / findById / findAll 等方法。
 *
 * <p>M1.2 起所有查询都带上 userId：
 * “按 id 查”改成“按 id + userId 查”，
 * 这样别人的会话查出来是空，天然就隔离了，也不会泄露“这个 id 存在”。
 */
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    /** 某个用户的会话列表，最近更新的排在前面。 */
    List<Conversation> findByUserIdOrderByUpdatedAtDesc(Long userId);

    /** 按 id + 归属用户查，防止越权访问别人的会话。 */
    Optional<Conversation> findByIdAndUserId(Long id, Long userId);
}
