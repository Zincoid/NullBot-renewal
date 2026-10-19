package com.zincoid.nullbot.bot.spi.event;

/**
 * Group chat poke event.
 * <p>群聊戳一戳事件。</p>
 *
 * @param selfId   the bot's id
 * @param userId   the poker's id
 * @param targetId the poked user's id
 * @param groupId  the group id
 */
public record GroupPokeEvent(
        long selfId,
        long userId,
        long targetId,
        long groupId
) implements PokeEvent {
}
