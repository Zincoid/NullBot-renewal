package com.zincoid.nullbot.console.dto;

/**
 * 控制台机器人账号视图。
 * <p>Console view of the bot account.</p>
 *
 * @param online   whether the bot is online
 * @param userId   the bot account id, null when offline
 * @param nickname the bot nickname, null when offline
 * @param avatar   the bot avatar url, nullable
 */
public record BotView(boolean online, String userId, String nickname, String avatar) {
}
