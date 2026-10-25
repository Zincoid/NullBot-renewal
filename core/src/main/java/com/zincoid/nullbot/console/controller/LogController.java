package com.zincoid.nullbot.console.controller;

import com.zincoid.nullbot.console.service.LogStreamService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 控制台日志接口：订阅实时日志流。
 * <p>Console log API: subscribes to the real-time log stream.</p>
 */
@RestController
@RequestMapping("/console/logs")
@SecurityRequirement(name = "console-token")
@RequiredArgsConstructor
public class LogController {

    private final LogStreamService logStreamService;

    // ════ Stream ═════════════════

    /**
     * 订阅实时日志流。
     * <p>Subscribes to the real-time log stream.</p>
     *
     * @param response the HTTP response
     * @return the server-sent event stream
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(HttpServletResponse response) {
        response.setHeader("X-Accel-Buffering", "no");
        response.setHeader("Cache-Control", "no-cache, no-transform");
        return logStreamService.subscribe();
    }
}
