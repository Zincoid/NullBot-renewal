package com.zincoid.nullbot.bot.spi.event;

/**
 * Group join event.
 * <p>入群事件。</p>
 *
 * @param selfId     the bot's id
 * @param groupId    the id of the group where the user joined
 * @param userId     the joining user's id
 * @param operatorId the operator who approved the join
 */
public record JoinEvent(
        long selfId,
        long groupId,
        long userId,
        long operatorId
) implements BotEvent {
}
