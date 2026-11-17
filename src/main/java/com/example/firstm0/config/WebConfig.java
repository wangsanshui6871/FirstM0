package com.example.firstm0.config;

import com.example.firstm0.auth.AuthInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring MVC 配置：注册拦截器。
 *
 * <p>规则：
 * <ul>
 *   <li>所有 {@code /api/**} 都要带 token；</li>
 *   <li>注册和登录本身不能要求登录，否则永远进不去；</li>
 *   <li>{@code /api/hello} 是骨架自检接口，保留公开，方便排查“服务是否活着”；</li>
 *   <li>静态页面和页面里的 JS/CSS 不在 /api 下，天然不受影响。</li>
 * </ul>
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;

    public WebConfig(AuthInterceptor authInterceptor) {
        this.authInterceptor = authInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/auth/**",
                        "/api/hello"
                );
    }
}
