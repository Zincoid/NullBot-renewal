package com.zincoid.nullbot.bot.input;

import com.zincoid.nullbot.module.system.SystemException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * 输入等待管理器：登记等待中的输入，匹配消息后交付或超时返回。
 * <p>Input wait manager: registers pending inputs, delivers matching messages, or returns on timeout.</p>
 */
@Slf4j
@Component
public class InputManager {

    private final Map<String, InputEntry> entries = new ConcurrentHashMap<>();
    private final Map<String, List<InputResult>> caches = new ConcurrentHashMap<>();

    private record InputEntry(CompletableFuture<List<InputResult>> future, Pattern pattern, boolean coverable) {
    }

    // ════ Request ═════════════════

    /**
     * 等待一次匹配输入，超时返回空列表。
     * <p>Waits for a matching input and returns an empty list on timeout.</p>
     *
     * @param mode     the wait mode
     * @param targetId the user or group platform id
     * @param pattern  the regular expression the message must match
     * @param timeout  the timeout in seconds
     * @return the matched inputs, empty on timeout
     */
    public List<InputResult> request(InputMode mode, String targetId, String pattern, long timeout) {
        return request(mode, targetId, pattern, timeout, false);
    }

    /**
     * 等待一次匹配输入，可选择覆盖已存在的可覆盖等待。
     * <p>Waits for a matching input, optionally covering an existing coverable wait.</p>
     *
     * @param mode      the wait mode
     * @param targetId  the user or group platform id
     * @param pattern   the regular expression the message must match
     * @param timeout   the timeout in seconds
     * @param coverable whether this wait may replace an existing coverable wait
     * @return the matched inputs, empty on timeout
     */
    public List<InputResult> request(InputMode mode, String targetId, String pattern, long timeout, boolean coverable) {
        if (mode == null) throw new SystemException("Missing key: mode");
        if (targetId == null || targetId.isBlank()) throw new SystemException("Missing key: targetId");
        if (pattern == null) throw new SystemException("Missing key: pattern");
        String key = key(mode, targetId);
        InputEntry previous = entries.get(key);
        if (previous != null) {
            if (!previous.coverable()) throw new SystemException("Input conflict: " + mode + " " + targetId);
            cancelWait(mode, targetId);
            log.info("Input wait covered: mode={}, target={}", mode, targetId);
        }
        if (mode == InputMode.GM)
            caches.put(key, Collections.synchronizedList(new ArrayList<>()));
        CompletableFuture<List<InputResult>> future = new CompletableFuture<>();
        entries.put(key, new InputEntry(future, Pattern.compile(pattern), coverable));
        try {
            log.info("Waiting for input: mode={}, target={}, timeout={}s", mode, targetId, timeout);
            return future.orTimeout(timeout, TimeUnit.SECONDS)
                    .exceptionally(_ -> {
                        log.info("Input timeout: mode={}, target={}", mode, targetId);
                        return mode == InputMode.GM
                                ? caches.getOrDefault(key, List.of())
                                : List.of();
                    })
                    .get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SystemException("Input wait interrupted", e);
        } catch (ExecutionException e) {
            throw new SystemException("Input wait failed", e);
        } finally {
            entries.remove(key);
            if (mode == InputMode.GM) caches.remove(key);
        }
    }

    // ════ Response ════════════════

    /**
     * 把群消息尝试交付给所有等待中的输入。
     * <p>Attempts to deliver a group message to all pending inputs.</p>
     *
     * @param groupId group platform id
     * @param userId  sender platform user id
     * @param message the message text
     * @return true when at least one wait accepted the message
     */
    public boolean response(String groupId, String userId, String message) {
        boolean responded = deliver(InputMode.PS, userId, userId, message);
        if (deliver(InputMode.GS, groupId, userId, message)) responded = true;
        if (deliver(InputMode.GM, groupId, userId, message)) responded = true;
        return responded;
    }

    /**
     * 把私聊消息尝试交付给该用户的等待输入。
     * <p>Attempts to deliver a private message to the user's pending input.</p>
     *
     * @param userId  sender platform user id
     * @param message the message text
     * @return true when a wait accepted the message
     */
    public boolean response(String userId, String message) {
        return deliver(InputMode.PS, userId, userId, message);
    }

    // ════ Helpers ═════════════════

    /**
     * 是否正在等待指定目标。
     * <p>Whether a wait is pending for the target.</p>
     *
     * @param mode     the wait mode
     * @param targetId the user or group platform id
     * @return true when a wait is pending
     */
    public boolean isWaiting(InputMode mode, String targetId) {
        InputEntry entry = entries.get(key(mode, targetId));
        return entry != null && !entry.future().isDone();
    }

    /**
     * 取消指定目标的等待，使其返回空列表。
     * <p>Cancels the wait for the target, making it return an empty list.</p>
     *
     * @param mode     the wait mode
     * @param targetId the user or group platform id
     */
    public void cancelWait(InputMode mode, String targetId) {
        String key = key(mode, targetId);
        InputEntry entry = entries.remove(key);
        if (entry == null || entry.future().isDone()) return;
        entry.future().complete(List.of());
    }

    private boolean deliver(InputMode mode, String targetId, String userId, String message) {
        String key = key(mode, targetId);
        InputEntry entry = entries.get(key);
        if (entry == null || entry.future().isDone()) return false;
        if (!entry.pattern().matcher(message).matches()) return false;
        if (mode == InputMode.GM) {
            List<InputResult> cache = caches.get(key);
            if (cache != null) cache.add(new InputResult(userId, message));
        } else {
            entry.future().complete(List.of(new InputResult(userId, message)));
        }
        log.info("Input received: mode={}, target={}, user={}, message={}", mode, targetId, userId, message);
        return true;
    }

    private static String key(InputMode mode, String targetId) {
        return mode + "_" + targetId;
    }
}
