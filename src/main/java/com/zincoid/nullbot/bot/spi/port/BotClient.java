package com.zincoid.nullbot.bot.spi.port;

import com.zincoid.nullbot.bot.spi.model.MsgContent;
import com.zincoid.nullbot.bot.spi.model.MsgDetail;
import com.zincoid.nullbot.bot.spi.model.UserInfo;

/**
 * Outbound port: bot capabilities available to business code.
 * <p>出站端口：向业务代码暴露的机器人能力。</p>
 */
public interface BotClient {

    // ════ Messaging ═══════════════

    /**
     * Sends a group message, returns the message ID.
     * <p>发送群聊消息，返回消息 ID。</p>
     */
    default long sendGroupMsg(long groupId, MsgContent content) {
        throw new UnsupportedSpiException("sendGroupMsg");
    }

    /**
     * Sends a plain-text group message, returns the message ID.
     * <p>发送纯文本群聊消息，返回消息 ID。</p>
     */
    default long sendGroupMsg(long groupId, String text) {
        return sendGroupMsg(groupId, MsgContent.of(text));
    }

    /**
     * Sends a private message, returns the message ID.
     * <p>发送私聊消息，返回消息 ID。</p>
     */
    default long sendPrivateMsg(long userId, MsgContent content) {
        throw new UnsupportedSpiException("sendPrivateMsg");
    }

    /**
     * Sends a plain-text private message, returns the message ID.
     * <p>发送纯文本私聊消息，返回消息 ID。</p>
     */
    default long sendPrivateMsg(long userId, String text) {
        return sendPrivateMsg(userId, MsgContent.of(text));
    }

    /**
     * Fetches a message by ID.
     * <p>按 ID 获取消息。</p>
     */
    default MsgDetail getMsg(long messageId) {
        throw new UnsupportedSpiException("getMsg");
    }

    /**
     * Recalls a message.
     * <p>撤回消息。</p>
     */
    default void recall(long messageId) {
        throw new UnsupportedSpiException("recall");
    }

    // ════ Poke ════════════════════

    /**
     * Pokes a user in a group chat.
     * <p>群聊戳一戳用户。</p>
     */
    default void pokeGroup(long groupId, long userId) {
        throw new UnsupportedSpiException("pokeGroup");
    }

    /**
     * Pokes a user in a private chat.
     * <p>私聊戳一戳用户。</p>
     */
    default void pokePrivate(long userId) {
        throw new UnsupportedSpiException("pokePrivate");
    }

    // ════ Mute ════════════════════

    /**
     * Mutes a group user, zero lifts the mute.
     * <p>禁言群用户，0 解除禁言。</p>
     */
    default void mute(long groupId, long userId, int seconds) {
        throw new UnsupportedSpiException("mute");
    }

    // ════ Info queries ════════════

    /**
     * Login info.
     * <p>登录信息。</p>
     */
    default UserInfo getLoginInfo() {
        throw new UnsupportedSpiException("getLoginInfo");
    }

    /**
     * User info.
     * <p>用户信息。</p>
     */
    default UserInfo getUserInfo(long userId) {
        throw new UnsupportedSpiException("getUserInfo");
    }
}
