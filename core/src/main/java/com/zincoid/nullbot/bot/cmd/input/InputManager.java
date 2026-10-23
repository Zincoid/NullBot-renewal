package com.zincoid.nullbot.bot.cmd.input;

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
import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;

/**
 * 输入等待管理器：登记等待中的输入，匹配消息后交付或超时返回。
 * <p>Input wait manager: registers pending inputs, delivers matching messages, or returns on timeout.</p>
 */
@Slf4j
@Component
public class InputManager {

    private final Map<String, InputEntry> entries = new ConcurrentHashMap<>();

    private record InputEntry(CompletableFuture<List<InputResult>> future, Pattern pattern,
                              boolean coverable, List<InputResult> values) {
    }

    // ════ Request ═════════════════

    /**
     * 等待一次匹配输入，超时返回已收到的输入。
     * <p>Waits for a matching input, returning the received inputs on timeout.</p>
     *
     * @param mode     the wait mode
     * @param targetId the user or group platform id
     * @param pattern  the regular expression the message must match
     * @param timeout  the timeout in seconds
     * @return the matched inputs, empty when none arrived
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
     * @return the matched inputs, empty when none arrived
     */
    public List<InputResult> request(InputMode mode, String targetId, String pattern, long timeout, boolean coverable) {
        if (mode == null) throw new InputException("Missing key: mode");
        if (targetId == null || targetId.isBlank()) throw new InputException("Missing key: targetId");
        if (pattern == null) throw new InputException("Missing key: pattern");
        String key = key(mode, targetId);
        InputEntry entry = new InputEntry(
                new CompletableFuture<>(),
                Pattern.compile(pattern),
                coverable,
                mode == InputMode.GM ? Collections.synchronizedList(new ArrayList<>()) : null);
        register(mode, targetId, key, entry);
        log.info("Waiting for input: mode={}, target={}, timeout={}s", mode, targetId, timeout);
        try {
            return entry.future().get(timeout, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            log.info("Input timeout: mode={}, target={}", mode, targetId);
            return values(entry);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InputException("Input wait interrupted", e);
        } catch (ExecutionException e) {
            throw new InputException("Input wait failed", e);
        } finally {
            entries.remove(key, entry);
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
     * 取消指定目标的等待，使其返回已收到的输入。
     * <p>Cancels the wait for the target, making it return the received inputs.</p>
     *
     * @param mode     the wait mode
     * @param targetId the user or group platform id
     */
    public void cancelWait(InputMode mode, String targetId) {
        InputEntry entry = entries.remove(key(mode, targetId));
        if (entry == null || entry.future().isDone()) return;
        entry.future().complete(values(entry));
    }

    /**
     * 原子地登记等待，必要时覆盖已有的可覆盖等待。
     * <p>Registers the wait atomically, covering an existing coverable wait when needed.</p>
     *
     * @param mode     the wait mode
     * @param targetId the user or group platform id
     * @param key      the wait key
     * @param entry    the wait entry
     */
    private void register(InputMode mode, String targetId, String key, InputEntry entry) {
        while (true) {
            InputEntry previous = entries.putIfAbsent(key, entry);
            if (previous == null) return;
            if (!previous.coverable())
                throw new InputException("Input conflict: " + mode + " " + targetId);
            if (!entries.replace(key, previous, entry)) continue;
            previous.future().complete(values(previous));
            log.info("Input wait covered: mode={}, target={}", mode, targetId);
            return;
        }
    }

    private boolean deliver(InputMode mode, String targetId, String userId, String message) {
        InputEntry entry = entries.get(key(mode, targetId));
        if (entry == null || entry.future().isDone()) return false;
        if (!entry.pattern().matcher(message).matches()) return false;
        InputResult result = new InputResult(userId, message);
        if (mode == InputMode.GM) {
            entry.values().add(result);
        } else {
            entry.future().complete(List.of(result));
        }
        log.info("Input received: mode={}, target={}, user={}, message={}", mode, targetId, userId, message);
        return true;
    }

    private static List<InputResult> values(InputEntry entry) {
        List<InputResult> values = entry.values();
        if (values == null) return List.of();
        synchronized (values) {
            return List.copyOf(values);
        }
    }

    private static String key(InputMode mode, String targetId) {
        return mode + "_" + targetId;
    }
}
