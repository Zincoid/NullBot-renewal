package com.zincoid.nullbot.bot.spi.event;

/**
 * 私聊消息撤回事件。
 * <p>Private chat message recall event.</p>
 *
 * @param selfId    the bot's id
 * @param userId    the recalled message's author id
 * @param messageId the recalled message id
 */
public record PrivateRecallEvent(
        String selfId,
        String userId,
        String messageId
) implements RecallEvent {
}
