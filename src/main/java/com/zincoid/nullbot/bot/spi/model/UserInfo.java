package com.zincoid.nullbot.bot.spi.model;

/**
 * 用户信息。
 * <p>User info.</p>
 *
 * @param userId   the user ID
 * @param nickname the display nickname
 */
public record UserInfo(String userId, String nickname) {
}
