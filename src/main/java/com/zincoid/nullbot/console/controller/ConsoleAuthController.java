package com.zincoid.nullbot.console.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 控制台令牌校验接口。
 * <p>Console token validation API.</p>
 */
@RestController
@RequestMapping("/console/auth")
@SecurityRequirement(name = "console-token")
public class ConsoleAuthController {

    /**
     * 校验请求头中的令牌，拦截器通过时返回 204。
     * <p>Validates the token in the request header, returning 204 when the interceptor passes.</p>
     *
     * @return an empty response
     */
    @GetMapping
    public ResponseEntity<Void> check() {
        return ResponseEntity.noContent().build();
    }
}
