package com.zincoid.nullbot.bot.spi.event;

/**
 * Group chat message recall event.
 * <p>群聊消息撤回事件。</p>
 *
 * @param selfId     the bot's id
 * @param userId     the recalled message's author id
 * @param messageId  the recalled message id
 * @param groupId    the group id
 * @param operatorId the id of the user who recalled the message
 */
public record GroupRecallEvent(
        long selfId,
        long userId,
        long messageId,
        long groupId,
        long operatorId
) implements RecallEvent {
}
