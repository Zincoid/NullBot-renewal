package com.zincoid.nullbot.bot.spi.event;

/**
 * 统一机器人事件模型。
 * <p>Unified bot event model.</p>
 */
public sealed interface BotEvent permits
        MsgEvent,
        PokeEvent,
        RecallEvent,
        JoinEvent,
        LeaveEvent {

    /**
     * 机器人 ID。
     * <p>The bot's id.</p>
     */
    String selfId();
}
