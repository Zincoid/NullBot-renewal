package com.zincoid.nullbot.bot;

import com.zincoid.nullbot.bot.spi.port.BotClient;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * 机器人出站端口辅助：查询名称并做 TTL 缓存，失败时回退为 ID。
 * <p>Helpers for the bot outbound port: look up names through a TTL cache and fall back to ids.</p>
 */
@Slf4j
public final class BotClients {

    private static final long USER_TTL_MILLIS = Duration.ofHours(1).toMillis();
    private static final long GROUP_TTL_MILLIS = Duration.ofHours(12).toMillis();
    private static final int MAX_ENTRIES = 2048;

    private static final Map<String, Cached> USER_NAMES = lru();
    private static final Map<String, Cached> GROUP_NAMES = lru();

    private BotClients() {
    }

    // ════ Name ════════════════════

    public static String userName(BotClient client, String userId) {
        return name(USER_NAMES, userId, USER_TTL_MILLIS,
                () -> client.getUserInfo(userId).nickname());
    }

    public static String groupName(BotClient client, String groupId) {
        return name(GROUP_NAMES, groupId, GROUP_TTL_MILLIS,
                () -> client.getGroupInfo(groupId).name());
    }

    // ════ Helpers ═════════════════

    private static String name(Map<String, Cached> cache, String id, long ttlMillis,
                               Supplier<String> query) {
        long now = System.currentTimeMillis();
        Cached cached = cache.get(id);
        if (cached != null) {
            if (cached.expiresAt() > now) return cached.value();
            cache.remove(id, cached);
        }
        try {
            String value = query.get();
            if (value == null || value.isBlank() || value.equals(id)) return id;
            cache.put(id, new Cached(value, now + ttlMillis));
            return value;
        } catch (RuntimeException e) {
            log.warn("Failed to query name: {}", id, e);
            return id;
        }
    }

    private static Map<String, Cached> lru() {
        return Collections.synchronizedMap(new LinkedHashMap<>(64, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, Cached> eldest) {
                return size() > MAX_ENTRIES;
            }
        });
    }

    private record Cached(String value, long expiresAt) {
    }
}
