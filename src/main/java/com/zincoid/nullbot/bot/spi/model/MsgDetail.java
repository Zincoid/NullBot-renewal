package com.zincoid.nullbot.bot.spi.model;

/**
 * 消息详情。
 * <p>Message detail.</p>
 *
 * @param messageId the message id
 * @param userId    the sender's id
 * @param content   the message content
 */
public record MsgDetail(String messageId, String userId, MsgContent content) {
}
