package com.zincoid.nullbot.bot.spi.event;

/**
 * Private chat poke event.
 * <p>私聊戳一戳事件。</p>
 *
 * @param selfId   the bot's id
 * @param userId   the poker's id
 * @param targetId the poked user's id
 */
public record PrivatePokeEvent(
        String selfId,
        String userId,
        String targetId
) implements PokeEvent {
}
