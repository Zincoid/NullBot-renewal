package com.zincoid.nullbot.bot.spi.event;

/**
 * 退群事件（主动退群或被踢）。
 * <p>Group leave event (left voluntarily or kicked).</p>
 *
 * @param selfId     the bot's id
 * @param groupId    the id of the group where the user left
 * @param userId     the leaving user's id
 * @param operatorId the id of the operator who kicked
 * @param kicked     whether the user was kicked
 */
public record LeaveEvent(
        String selfId,
        String groupId,
        String userId,
        String operatorId,
        boolean kicked
) implements BotEvent, GroupEvent {
}
