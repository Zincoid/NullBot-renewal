package com.zincoid.nullbot.bot.spi.event;

/**
 * Message recall event (group / private chat).
 * <p>消息撤回事件（群聊 / 私聊）。</p>
 */
public sealed interface RecallEvent extends BotEvent permits
        GroupRecallEvent,
        PrivateRecallEvent {

    /**
     * The recalled message's author id.
     * <p>被撤回消息的发送者 ID。</p>
     */
    String userId();

    /**
     * The recalled message id.
     * <p>被撤回的消息 ID。</p>
     */
    String messageId();
}
