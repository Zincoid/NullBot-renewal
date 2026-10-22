package com.zincoid.nullbot.bot.spi.model;

/**
 * 用户信息。
 * <p>User info.</p>
 *
 * @param userId   the user id
 * @param nickname the display nickname
 * @param avatar   the avatar url, nullable
 */
public record UserInfo(String userId, String nickname, String avatar) {
}
