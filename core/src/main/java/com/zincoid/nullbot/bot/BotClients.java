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

    /**
     * 查询用户昵称，失败时回退为 ID。
     * <p>Looks up the user nickname, falling back to the id.</p>
     *
     * @param client the outbound port
     * @param userId platform user id
     * @return the nickname or the id
     */
    public static String nickname(BotClient client, String userId) {
        return lookup(userId, "user info", () -> client.getUserInfo(userId).nickname());
    }

    /**
     * 查询群名，失败时回退为群 ID。
     * <p>Looks up the group name, falling back to the group id.</p>
     *
     * @param client  the outbound port
     * @param groupId platform group id
     * @return the group name or the group id
     */
    public static String groupName(BotClient client, String groupId) {
        return lookup(groupId, "group info", () -> client.getGroupInfo(groupId).name());
    }

    /**
     * 查询机器人昵称，失败时回退为 ID。
     * <p>Looks up the bot nickname, falling back to the id.</p>
     *
     * @param client the outbound port
     * @param selfId the bot's platform user id
     * @return the nickname or the id
     */
    public static String selfName(BotClient client, String selfId) {
        return lookup(selfId, "login info", () -> client.getLoginInfo().nickname());
    }

    // ════ Helpers ═════════════════

    private static String lookup(String id, String target, Supplier<String> query) {
        try {
            String name = query.get();
            return name == null || name.isBlank() ? id : name;
        } catch (RuntimeException e) {
            log.warn("Failed to query {}: {}", target, id, e);
            return id;
        }
    }
}
