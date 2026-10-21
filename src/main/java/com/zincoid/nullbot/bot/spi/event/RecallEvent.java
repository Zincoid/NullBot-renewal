package com.zincoid.nullbot.bot.spi.event;

/**
 * 消息撤回事件（群聊 / 私聊）。
 * <p>Message recall event (group / private chat).</p>
 */
public sealed interface RecallEvent extends BotEvent permits
        GroupRecallEvent,
        PrivateRecallEvent {

    /**
     * 被撤回消息的发送者 ID。
     * <p>The recalled message's author id.</p>
     */
    String userId();

    /**
     * 被撤回的消息 ID。
     * <p>The recalled message id.</p>
     */
    String messageId();
}
