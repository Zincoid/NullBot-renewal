package com.zincoid.nullbot.adapter.shiro;

import com.mikuac.shiro.core.Bot;
import com.mikuac.shiro.dto.action.common.ActionData;
import com.mikuac.shiro.dto.action.common.ActionRaw;
import com.mikuac.shiro.dto.action.common.MsgId;
import com.mikuac.shiro.dto.action.response.MsgResp;
import com.zincoid.nullbot.bot.spi.model.MsgContent;
import com.zincoid.nullbot.bot.spi.model.MsgDetail;
import com.zincoid.nullbot.bot.spi.model.UserInfo;
import com.zincoid.nullbot.bot.spi.port.BotClient;

/**
 * Shiro outbound adapter: over the Shiro {@link Bot}.
 * <p>Shiro 出站适配器：基于 Shiro {@link Bot}。</p>
 */
public class ShiroClient implements BotClient {

    private final Bot bot;

    public ShiroClient(Bot bot) {
        this.bot = bot;
    }

    // ════ Messaging ═══════════════

    @Override
    public long sendGroupMsg(long groupId, MsgContent content) {
        ActionData<MsgId> resp = bot.sendGroupMsg(groupId, ShiroMapper.toShiro(content), false);
        return required(check(resp).getMessageId(), "messageId");
    }

    @Override
    public long sendPrivateMsg(long userId, MsgContent content) {
        ActionData<MsgId> resp = bot.sendPrivateMsg(userId, ShiroMapper.toShiro(content), false);
        return required(check(resp).getMessageId(), "messageId");
    }

    @Override
    public MsgDetail getMsg(long messageId) {
        MsgResp resp = check(bot.getMsg((int) messageId));
        return new MsgDetail(
                messageId, required(resp.getUserId(), "userId"),
                ShiroMapper.fromShiro(required(resp.getArrayMsg(), "arrayMsg"))
        );
    }

    @Override
    public void recall(long messageId) {
        check(bot.deleteMsg((int) messageId));
    }

    // ════ Poke ════════════════════

    @Override
    public void pokeGroup(long groupId, long userId) {
        check(bot.sendGroupPoke(groupId, userId));
    }

    @Override
    public void pokePrivate(long userId) {
        check(bot.sendFriendPoke(userId));
    }

    // ════ Mute ════════════════════

    @Override
    public void mute(long groupId, long userId, int seconds) {
        check(bot.setGroupBan(groupId, userId, seconds));
    }

    // ════ Info queries ════════════

    @Override
    public UserInfo getLoginInfo() {
        var resp = check(bot.getLoginInfo());
        return new UserInfo(
                required(resp.getUserId(), "userId"),
                required(resp.getNickname(), "nickname")
        );
    }

    @Override
    public UserInfo getUserInfo(long userId) {
        var resp = check(bot.getStrangerInfo(userId, true));
        return new UserInfo(
                required(resp.getUserId(), "userId"),
                required(resp.getNickname(), "nickname")
        );
    }

    // ════ Helpers ═════════════════

    private static <T> T required(T value, String name) {
        if (value == null) throw new ShiroException("Missing value: " + name);
        return value;
    }

    private static <T> T check(ActionData<T> resp) {
        checkCode(resp == null ? null : resp.getRetCode());
        return required(resp.getData(), "data");
    }

    private static void check(ActionRaw resp) {
        checkCode(resp == null ? null : resp.getRetCode());
    }

    private static void checkCode(Integer retCode) {
        if (retCode == null || retCode != 0)
            throw new ShiroException("Call failed: retCode=" + retCode);
    }
}
