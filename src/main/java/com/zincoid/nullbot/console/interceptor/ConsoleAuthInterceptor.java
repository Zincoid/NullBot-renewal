package com.zincoid.nullbot.console.interceptor;

import com.zincoid.nullbot.console.exception.ConsoleException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * 校验控制台接口的静态令牌。
 * <p>Validates the static token of the console API.</p>
 */
@Slf4j
@RequiredArgsConstructor
public class ConsoleAuthInterceptor implements HandlerInterceptor {

    private static final String TOKEN_HEADER = "token";

    private final String token;

    /**
     * 校验请求头中的令牌。
     * <p>Checks the token in the request header.</p>
     *
     * @param request  the HTTP request
     * @param response the HTTP response
     * @param handler  the handler
     * @return true when the request may proceed
     */
    @Override
    public boolean preHandle(@NonNull HttpServletRequest request,
                             @NonNull HttpServletResponse response,
                             @NonNull Object handler) {
        String provided = request.getHeader(TOKEN_HEADER);
        if (provided != null && MessageDigest.isEqual(
                provided.getBytes(StandardCharsets.UTF_8),
                token.getBytes(StandardCharsets.UTF_8)))
            return true;
        log.warn("Console request denied: {} {}", request.getMethod(), request.getRequestURI());
        throw new ConsoleException(HttpStatus.UNAUTHORIZED, "Invalid console token");
    }
}
