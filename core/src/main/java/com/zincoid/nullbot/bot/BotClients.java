package com.zincoid.nullbot.bot;

import com.zincoid.nullbot.bot.spi.port.BotClient;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Supplier;

/**
 * 机器人出站端口辅助：查询名称，失败时回退为 ID。
 * <p>Helpers for the bot outbound port: look up names and fall back to ids.</p>
 */
@Slf4j
public final class BotClients {

    private BotClients() {
    }

    // ════ Name ════════════════════

    public static String userName(BotClient client, String userId) {
        return name(userId, () -> client.getUserInfo(userId).nickname());
    }

    public static String groupName(BotClient client, String groupId) {
        return name(groupId, () -> client.getGroupInfo(groupId).name());
    }

    public static String selfName(BotClient client, String selfId) {
        return name(selfId, () -> client.getLoginInfo().nickname());
    }

    // ════ Helpers ═════════════════

    private static String name(String id, Supplier<String> query) {
        try {
            String name = query.get();
            return name == null || name.isBlank() ? id : name;
        } catch (RuntimeException e) {
            log.warn("Failed to query name: {}", id, e);
            return id;
        }
    }
}
