package com.zincoid.nullbot.bot.spi.event;

import com.zincoid.nullbot.bot.spi.model.MsgContent;

/**
 * Group chat message event.
 * <p>群聊消息事件。</p>
 *
 * @param selfId     the bot's id
 * @param userId     the sender's id
 * @param groupId    the group id
 * @param messageId  the message id
 * @param nickname   the sender's nickname
 * @param content    the message content
 * @param at         whether the bot is mentioned
 * @param reply      whether the first segment is a reply quote
 */
public record GroupMsgEvent(
        long selfId,
        long userId,
        long groupId,
        long messageId,
        String nickname,
        MsgContent content,
        boolean at,
        boolean reply
) implements MsgEvent {
}
