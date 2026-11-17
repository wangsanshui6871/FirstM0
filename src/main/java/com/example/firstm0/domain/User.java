package com.example.firstm0.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * 用户实体，对应数据库表 app_user。
 *
 * <p>为什么不叫 user？
 * MySQL 里 {@code user} 虽然不算保留字，但它和系统库 {@code mysql.user} 同名，
 * 排查问题时容易看混；换成 app_user 更明确，也是不少项目的惯例。
 *
 * <p>注意：这里只存密码的哈希值（BCrypt 算出来的），
 * 永远不存明文密码，也不要把 passwordHash 返回给前端。
 */
@Entity
@Table(name = "app_user")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 登录名，唯一。 */
    @Column(nullable = false, unique = true, length = 50)
    private String username;

    /** BCrypt 哈希，形如 $2a$10$...，长度固定 60，留 100 够用。 */
    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "varchar(20)")
    private UserRole role;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /** JPA 要求的无参构造器。 */
    protected User() {
    }

    public User(String username, String passwordHash, UserRole role, LocalDateTime now) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
        this.createdAt = now;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public UserRole getRole() {
        return role;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
