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

    /**
     * 事件关联用户 ID，具体语义由事件类型决定。
     * <p>The user id associated with the event; its meaning depends on the event type.</p>
     *
     * @return the user id
     */
    String userId();
}
