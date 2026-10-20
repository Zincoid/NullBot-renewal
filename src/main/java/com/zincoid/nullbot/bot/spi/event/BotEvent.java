package com.zincoid.nullbot.bot.spi.event;

/**
 * Unified bot event model.
 * <p>统一机器人事件模型。</p>
 */
public sealed interface BotEvent permits
        MsgEvent,
        PokeEvent,
        RecallEvent,
        JoinEvent,
        LeaveEvent {

    /**
     * The bot's id.
     * <p>机器人 ID。</p>
     */
    String selfId();
}
