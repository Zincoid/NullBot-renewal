package com.zincoid.nullbot.bot.spi.event;

import com.zincoid.nullbot.bot.spi.model.MsgContent;

/**
 * Message event (group / private chat).
 * <p>消息事件（群聊 / 私聊）。</p>
 */
public sealed interface MsgEvent extends BotEvent permits
        GroupMsgEvent,
        PrivateMsgEvent {

    /**
     * The sender's id.
     * <p>发送者 ID。</p>
     */
    long userId();

    /**
     * The message id.
     * <p>消息 ID。</p>
     */
    long messageId();

    /**
     * The sender's nickname.
     * <p>发送者昵称。</p>
     */
    String nickname();

    /**
     * The message content.
     * <p>消息内容。</p>
     */
    MsgContent content();

    /**
     * Whether the bot is mentioned.
     * <p>是否 @ 了机器人。</p>
     */
    boolean at();

    /**
     * Whether the first segment is a reply quote.
     * <p>首段是否为回复引用。</p>
     */
    boolean reply();
}
