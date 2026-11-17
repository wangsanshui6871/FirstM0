package com.example.firstm0.service;

import com.example.firstm0.auth.JwtService;
import com.example.firstm0.common.InvalidCredentialsException;
import com.example.firstm0.common.UsernameTakenException;
import com.example.firstm0.domain.User;
import com.example.firstm0.domain.UserRole;
import com.example.firstm0.dto.AuthResponse;
import com.example.firstm0.dto.LoginRequest;
import com.example.firstm0.dto.RegisterRequest;
import com.example.firstm0.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 注册与登录。
 *
 * <p>两个关键点：
 * <ol>
 *   <li>密码只存哈希。BCrypt 每次加密都会随机加盐，
 *       同一个密码两次加密的结果不一样，所以不能用“加密后字符串相等”来比对，
 *       必须用 {@code matches(明文, 哈希)}。</li>
 *   <li>登录失败只返回一句“用户名或密码错误”，
 *       不告诉对方到底是用户不存在还是密码错，避免被用来探测账号。</li>
 * </ol>
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, JwtService jwtService) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        // BCrypt 默认强度 10，加密一次约几十毫秒，对登录接口来说可以接受
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    /**
     * 注册新用户，成功则直接返回 token（省得再登录一次）。
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String username = request.username().strip();

        if (userRepository.existsByUsername(username)) {
            throw new UsernameTakenException(username);
        }

        String hash = passwordEncoder.encode(request.password());
        User user = userRepository.save(
                new User(username, hash, UserRole.USER, LocalDateTime.now()));

        log.info("新用户注册成功，userId={}", user.getId());

        return toResponse(user);
    }

    /**
     * 登录校验。
     */
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.username().strip())
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        return toResponse(user);
    }

    private AuthResponse toResponse(User user) {
        String token = jwtService.issue(user.getId(), user.getUsername());

        return new AuthResponse(token, user.getId(), user.getUsername(), user.getRole());
    }
}
