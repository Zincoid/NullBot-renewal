package com.zincoid.nullbot.module.setting;

import com.zincoid.nullbot.platform.persistence.po.GroupSetting;
import io.github.bucket4j.Bucket;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 指令限速器：按群设置对指令消费做令牌桶限速。
 * <p>Command rate limiter: token-bucket limiting driven by group settings.</p>
 */
@Component
@RequiredArgsConstructor
public class GroupCmdRateLimiter {

    private static final long SPAM_INTERVAL = 500;
    private static final int CLEANUP_THRESHOLD = 2048;
    private static final long CLEANUP_INTERVAL_NANOS = Duration.ofMinutes(1).toNanos();

    private final GroupSettingService groupSettingService;
    private final Map<String, BucketEntry> buckets = new ConcurrentHashMap<>();
    private final Map<String, Long> lastProcess = new ConcurrentHashMap<>();
    private final AtomicLong lastCleanup = new AtomicLong(System.nanoTime());

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
        AtomicBoolean spam = new AtomicBoolean();
        lastProcess.compute(groupId, (_, last) -> {
            if (last != null && now - last < SPAM_INTERVAL) {
                spam.set(true);
                return last;
            }
            return now;
        });
        return spam.get();
    }

    private Bucket resolveBucket(String key, GroupSetting setting) {
        BucketEntry entry = buckets.computeIfAbsent(key, _ -> new BucketEntry(
                Bucket.builder()
                        .addLimit(limit -> limit
                                .capacity(setting.getLimitCapacity())
                                .refillGreedy(setting.getLimitRefill(),
                                        Duration.ofMinutes(setting.getLimitInterval())))
                        .build(),
                setting.getLimitCapacity()));
        cleanup();
        return entry.bucket();
    }

    private void cleanup() {
        if (buckets.size() < CLEANUP_THRESHOLD) return;
        long now = System.nanoTime();
        long last = lastCleanup.get();
        if (now - last < CLEANUP_INTERVAL_NANOS
                || !lastCleanup.compareAndSet(last, now)) return;
        buckets.entrySet().removeIf(entry ->
                entry.getValue().bucket().getAvailableTokens() >= entry.getValue().capacity());
    }

    /**
     * 限速桶及其容量，容量用于判断桶是否已回满。
     * <p>A rate-limit bucket and its capacity; the capacity detects a full bucket.</p>
     *
     * @param bucket   the token bucket
     * @param capacity the bucket capacity
     */
    private record BucketEntry(Bucket bucket, int capacity) {
    }
}
