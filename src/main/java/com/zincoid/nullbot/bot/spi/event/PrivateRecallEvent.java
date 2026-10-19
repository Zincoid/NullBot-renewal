package com.zincoid.nullbot.bot.spi.event;

/**
 * Private chat message recall event.
 * <p>私聊消息撤回事件。</p>
 *
 * @param selfId    the bot's id
 * @param userId    the recalled message's author id
 * @param messageId the recalled message id
 */
public record PrivateRecallEvent(
        long selfId,
        long userId,
        long messageId
) implements RecallEvent {
}
