package com.zincoid.nullbot.bot.spi.model;

/**
 * Message detail.
 * <p>消息详情。</p>
 *
 * @param messageId the message id
 * @param userId    the sender's id
 * @param content   the message content
 */
public record MsgDetail(String messageId, String userId, MsgContent content) {
}
