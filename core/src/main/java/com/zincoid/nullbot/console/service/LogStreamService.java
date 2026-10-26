package com.zincoid.nullbot.console.service;

import ch.qos.logback.classic.spi.ILoggingEvent;
import com.zincoid.nullbot.console.dto.LogView;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 控制台日志流服务：维护 SSE 订阅并向浏览器广播日志事件。
 * <p>Console log stream service: keeps SSE subscriptions and broadcasts logging events to browsers.</p>
 */
@Slf4j
@Service
public class LogStreamService {

    private static final long HEARTBEAT_DELAY = 15_000L;

    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    /**
     * 注册一个日志流订阅。
     * <p>Registers a log stream subscription.</p>
     *
     * @return the subscribed emitter
     */
    public SseEmitter subscribe() {
        String id = UUID.randomUUID().toString();
        SseEmitter emitter = new SseEmitter(0L);
        emitters.put(id, emitter);
        emitter.onCompletion(() -> remove(id));
        emitter.onTimeout(() -> remove(id));
        emitter.onError(error -> remove(id));
        try {
            emitter.send(SseEmitter.event().name("connected").data("ok"));
        } catch (IOException e) {
            remove(id);
        }
        log.info("Log stream subscribed: {}", id);
        return emitter;
    }

    /**
     * 关闭所有日志流，避免优雅停机等待长连接超时。
     * <p>Closes all log streams so graceful shutdown does not wait for long-lived connections to time out.</p>
     */
    @EventListener(ContextClosedEvent.class)
    @PreDestroy
    public void close() {
        int count = emitters.size();
        emitters.values().forEach(emitter -> {
            try {
                emitter.complete();
            } catch (RuntimeException e) {
                log.debug("Log stream close failed", e);
            }
        });
        emitters.clear();
        if (count > 0) log.info("Log streams closed: {}", count);
    }

    /**
     * 向所有订阅者广播一条日志事件。
     * <p>Broadcasts a logging event to every subscriber.</p>
     *
     * @param event the logging event
     */
    public void broadcast(ILoggingEvent event) {
        if (emitters.isEmpty()) return;
        LogView view = LogView.of(event);
        emitters.forEach((id, emitter) -> send(id, emitter, SseEmitter.event().data(view)));
    }

    /**
     * 定期发送 SSE 注释以保持连接。
     * <p>Periodically sends an SSE comment to keep connections alive.</p>
     */
    @Scheduled(fixedDelay = HEARTBEAT_DELAY)
    public void heartbeat() {
        if (emitters.isEmpty()) return;
        emitters.forEach((id, emitter) ->
                send(id, emitter, SseEmitter.event().comment("keep-alive")));
    }

    // ════ Helpers ═════════════════

    private void send(String id, SseEmitter emitter, SseEmitter.SseEventBuilder event) {
        try {
            emitter.send(event);
        } catch (Exception e) {
            // A failed send means the client is gone; never let it reach the logging thread.
            remove(id);
        }
    }

    private void remove(String id) {
        if (emitters.remove(id) == null) return;
        log.info("Log stream unsubscribed: {}", id);
    }
}
