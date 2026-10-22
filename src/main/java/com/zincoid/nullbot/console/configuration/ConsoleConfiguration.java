package com.zincoid.nullbot.console.configuration;

import com.zincoid.nullbot.console.interceptor.ConsoleAuthInterceptor;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 注册控制台接口的令牌拦截器。
 * <p>Registers the token interceptor of the console API.</p>
 */
@Configuration
@RequiredArgsConstructor
public class ConsoleConfiguration implements WebMvcConfigurer {

    private final ConsoleProperties properties;

    /**
     * 把令牌拦截器挂到 {@code /console/**}。
     * <p>Attaches the token interceptor to {@code /console/**}.</p>
     *
     * @param registry interceptor registry
     */
    @Override
    public void addInterceptors(@NonNull InterceptorRegistry registry) {
        String token = properties.getToken();
        if (token == null || token.isBlank())
            throw new IllegalStateException("Missing console token: bot.console.token");
        registry.addInterceptor(new ConsoleAuthInterceptor(token))
                .addPathPatterns("/console/**");
    }
}
