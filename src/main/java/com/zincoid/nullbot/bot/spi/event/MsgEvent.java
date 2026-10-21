package com.zincoid.nullbot.bot.spi.event;

import com.zincoid.nullbot.bot.spi.model.MsgContent;

/**
 * 消息事件（群聊 / 私聊）。
 * <p>Message event (group / private chat).</p>
 */
public sealed interface MsgEvent extends BotEvent permits
        GroupMsgEvent,
        PrivateMsgEvent {

    /**
     * 发送者 ID。
     * <p>The sender's id.</p>
     */
    String userId();

    /**
     * 消息 ID。
     * <p>The message id.</p>
     */
    String messageId();

    /**
     * 发送者昵称。
     * <p>The sender's nickname.</p>
     */
    String nickname();

    /**
     * 消息内容。
     * <p>The message content.</p>
     */
    MsgContent content();

    /**
     * 是否 @ 了机器人。
     * <p>Whether the bot is mentioned.</p>
     */
    boolean at();

    /**
     * 首段是否为回复引用。
     * <p>Whether the first segment is a reply quote.</p>
     */
    boolean reply();
}
