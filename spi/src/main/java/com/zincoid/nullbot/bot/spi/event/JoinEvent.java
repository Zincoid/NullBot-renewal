package com.zincoid.nullbot.bot.spi.event;

/**
 * 入群事件。
 * <p>Group join event.</p>
 *
 * @param selfId     the bot's id
 * @param groupId    the id of the group where the user joined
 * @param userId     the joining user's id
 * @param operatorId the operator who approved the join
 */
public record JoinEvent(
        String selfId,
        String groupId,
        String userId,
        String operatorId
) implements BotEvent, GroupEvent {
}
