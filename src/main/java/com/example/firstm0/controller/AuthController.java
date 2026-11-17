package com.example.firstm0.controller;

import com.example.firstm0.dto.AuthResponse;
import com.example.firstm0.dto.LoginRequest;
import com.example.firstm0.dto.RegisterRequest;
import com.example.firstm0.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 注册与登录接口。
 *
 * <p>这两个接口不需要登录就能访问，M1.2 加拦截器时会把它们排除在外。
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /** 注册成功返回 201（Created），比 200 更准确地表达“新建了资源”。 */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }
}
