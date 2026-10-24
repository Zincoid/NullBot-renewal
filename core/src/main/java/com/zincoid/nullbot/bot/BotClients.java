package com.zincoid.nullbot.bot;

import com.zincoid.nullbot.bot.spi.port.BotClient;

/**
 * 机器人出站端口辅助：查询名称，失败时回退为 ID。
 * <p>Helpers for the bot outbound port: look up names and fall back to ids.</p>
 */
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
        try {
            return client.getUserInfo(userId).nickname();
        } catch (RuntimeException e) {
            return userId;
        }
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
        try {
            return client.getLoginInfo().nickname();
        } catch (RuntimeException e) {
            return selfId;
        }
    }
}
