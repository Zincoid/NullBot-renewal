package com.zincoid.nullbot.module.setting;

import com.zincoid.nullbot.platform.persistence.po.GroupSetting;
import io.github.bucket4j.Bucket;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 指令限速器：按群设置对指令消费做令牌桶限速。
 * <p>Command rate limiter: token-bucket limiting driven by group settings.</p>
 */
@Component
@RequiredArgsConstructor
public class GroupCmdRateLimiter {

    private static final long SPAM_INTERVAL = 500;

    private final GroupSettingService groupSettingService;
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final Map<String, Long> lastProcess = new ConcurrentHashMap<>();

    // ════ Consume ═════════════════

    /**
     * 尝试消费一个指令令牌。
     * <p>Tries to consume one command token.</p>
     *
     * @param groupId group platform id
     * @param userId  operator platform user id
     * @param cmd     command name
     * @return true when the call is allowed
     */
    public boolean tryConsume(String groupId, String userId, String cmd) {
        if (isSpam(groupId)) return false;
        GroupSetting setting = groupSettingService.get(groupId);
        String key = switch (setting.getLimitScope()) {
            case GROUP -> "[%s]".formatted(groupId);
            case USER -> "[%s][User:%s]".formatted(groupId, userId);
            case CMD -> "[%s][Cmd:%s]".formatted(groupId, cmd);
        };
        return resolveBucket(key, setting).tryConsume(1);
    }

    // ════ Reset ═══════════════════

    /**
     * 清除该群缓存的令牌桶与防抖时间。
     * <p>Clears the cached buckets and the spam timestamp of the group.</p>
     *
     * @param groupId group platform id
     */
    public void reset(String groupId) {
        String prefix = "[" + groupId + "]";
        buckets.keySet().removeIf(key -> key.startsWith(prefix));
        lastProcess.remove(groupId);
    }

    // ════ Helpers ═════════════════

    private boolean isSpam(String groupId) {
        long now = System.currentTimeMillis();
        long last = lastProcess.getOrDefault(groupId, 0L);
        if (now - last < SPAM_INTERVAL) return true;
        lastProcess.put(groupId, now);
        return false;
    }

    private Bucket resolveBucket(String key, GroupSetting setting) {
        return buckets.computeIfAbsent(key, _ -> Bucket.builder()
                .addLimit(limit -> limit
                        .capacity(setting.getLimitCapacity())
                        .refillGreedy(setting.getLimitRefill(),
                                Duration.ofMinutes(setting.getLimitInterval())))
                .build());
    }
}
