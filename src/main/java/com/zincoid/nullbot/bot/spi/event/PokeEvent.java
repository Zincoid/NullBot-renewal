package com.zincoid.nullbot.bot.spi.event;

/**
 * 戳一戳事件（群聊 / 私聊）。
 * <p>Poke event (group / private chat).</p>
 */
public sealed interface PokeEvent extends BotEvent permits
        GroupPokeEvent,
        PrivatePokeEvent {

    /**
     * 戳人者 ID。
     * <p>The poker's id.</p>
     */
    String userId();

    /**
     * 被戳者 ID。
     * <p>The poked user's id.</p>
     */
    String targetId();
}
