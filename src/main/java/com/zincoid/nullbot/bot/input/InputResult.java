package com.zincoid.nullbot.bot.input;

/**
 * 一次匹配到的输入：发送者与消息文本。
 * <p>A matched input: the sender and the message text.</p>
 *
 * @param userId  the sender platform user id
 * @param message the message text
 */
public record InputResult(String userId, String message) {
}
