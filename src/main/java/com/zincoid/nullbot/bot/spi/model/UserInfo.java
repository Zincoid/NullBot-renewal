package com.zincoid.nullbot.bot.spi.model;

/**
 * User info.
 * <p>用户信息。</p>
 *
 * @param userId   the user ID
 * @param nickname the display nickname
 */
public record UserInfo(long userId, String nickname) {
}
