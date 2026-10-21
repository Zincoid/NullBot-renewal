package com.zincoid.nullbot.bot.spi.port;

import com.zincoid.nullbot.bot.spi.model.GroupInfo;
import com.zincoid.nullbot.bot.spi.model.MsgContent;
import com.zincoid.nullbot.bot.spi.model.MsgDetail;
import com.zincoid.nullbot.bot.spi.model.UserInfo;

/**
 * 出站端口：向业务代码暴露的机器人能力。
 * <p>Outbound port: bot capabilities available to business code.</p>
 */
public interface BotClient {

    // ════ Messaging ═══════════════

    /**
     * 发送群聊消息，返回消息 ID。
     * <p>Sends a group message, returns the message id.</p>
     */
    default String sendGroupMsg(String groupId, MsgContent content) {
        throw new UnsupportedSpiException("sendGroupMsg");
    }

    /**
     * 发送纯文本群聊消息，返回消息 ID。
     * <p>Sends a plain-text group message, returns the message id.</p>
     */
    default String sendGroupMsg(String groupId, String text) {
        return sendGroupMsg(groupId, MsgContent.of(text));
    }

    /**
     * 发送私聊消息，返回消息 ID。
     * <p>Sends a private message, returns the message id.</p>
     */
    default String sendPrivateMsg(String userId, MsgContent content) {
        throw new UnsupportedSpiException("sendPrivateMsg");
    }

    /**
     * 发送纯文本私聊消息，返回消息 ID。
     * <p>Sends a plain-text private message, returns the message id.</p>
     */
    default String sendPrivateMsg(String userId, String text) {
        return sendPrivateMsg(userId, MsgContent.of(text));
    }

    /**
     * 按 ID 获取消息。
     * <p>Fetches a message by id.</p>
     */
    default MsgDetail getMsg(String messageId) {
        throw new UnsupportedSpiException("getMsg");
    }

    /**
     * 撤回消息。
     * <p>Recalls a message.</p>
     */
    default void recall(String messageId) {
        throw new UnsupportedSpiException("recall");
    }

    // ════ Poke ════════════════════

    /**
     * 群聊戳一戳用户。
     * <p>Pokes a user in a group chat.</p>
     */
    default void pokeGroup(String groupId, String userId) {
        throw new UnsupportedSpiException("pokeGroup");
    }

    /**
     * 私聊戳一戳用户。
     * <p>Pokes a user in a private chat.</p>
     */
    default void pokePrivate(String userId) {
        throw new UnsupportedSpiException("pokePrivate");
    }

    // ════ Mute ════════════════════

    /**
     * 禁言群用户，0 解除禁言。
     * <p>Mutes a group user, zero lifts the mute.</p>
     */
    default void mute(String groupId, String userId, int seconds) {
        throw new UnsupportedSpiException("mute");
    }

    // ════ Info queries ════════════

    /**
     * 登录信息。
     * <p>Login info.</p>
     */
    default UserInfo getLoginInfo() {
        throw new UnsupportedSpiException("getLoginInfo");
    }

    /**
     * 用户信息。
     * <p>User info.</p>
     */
    default UserInfo getUserInfo(String userId) {
        throw new UnsupportedSpiException("getUserInfo");
    }

    /**
     * 按 ID 获取群信息。
     * <p>Fetches group info by id.</p>
     */
    default GroupInfo getGroupInfo(String groupId) {
        throw new UnsupportedSpiException("getGroupInfo");
    }
}
