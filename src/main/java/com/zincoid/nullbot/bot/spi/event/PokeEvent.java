package com.zincoid.nullbot.bot.spi.event;

/**
 * Poke event (group / private chat).
 * <p>戳一戳事件（群聊 / 私聊）。</p>
 */
public sealed interface PokeEvent extends BotEvent permits
        GroupPokeEvent,
        PrivatePokeEvent {

    /**
     * The poker's id.
     * <p>戳人者 ID。</p>
     */
    String userId();

    /**
     * The poked user's id.
     * <p>被戳者 ID。</p>
     */
    String targetId();
}
