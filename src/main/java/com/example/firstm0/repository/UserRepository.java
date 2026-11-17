package com.example.firstm0.repository;

import com.example.firstm0.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 用户表的数据库操作。
 *
 * <p>existsByUsername 和 findByUsername 都是方法名派生查询，
 * 不用写 SQL：Spring Data JPA 会按名字生成对应语句。
 */
public interface UserRepository extends JpaRepository<User, Long> {

    /** 注册时用来判断用户名有没有被占用。 */
    boolean existsByUsername(String username);

    /** 登录时按用户名查人；查不到说明这个用户不存在。 */
    Optional<User> findByUsername(String username);
}
