package com.zincoid.nullbot.adapter.shiro;

import com.mikuac.shiro.core.Bot;
import com.mikuac.shiro.dto.action.common.ActionData;
import com.mikuac.shiro.dto.action.common.ActionRaw;
import com.mikuac.shiro.dto.action.common.MsgId;
import com.mikuac.shiro.dto.action.response.MsgResp;
import com.zincoid.nullbot.bot.spi.model.GroupInfo;
import com.zincoid.nullbot.bot.spi.model.MsgContent;
import com.zincoid.nullbot.bot.spi.model.MsgDetail;
import com.zincoid.nullbot.bot.spi.model.UserInfo;
import com.zincoid.nullbot.bot.spi.port.BotClient;

/**
 * Shiro 出站适配器：基于 Shiro {@link Bot}。
 * <p>Shiro outbound adapter: over the Shiro {@link Bot}.</p>
 */
public class ShiroClient implements BotClient {

    private final Bot bot;

    public ShiroClient(Bot bot) {
        this.bot = bot;
    }

    // ════ Messaging ═══════════════

    @Override
    public String sendGroupMsg(String groupId, MsgContent content) {
        ActionData<MsgId> resp = bot.sendGroupMsg(num(groupId), ShiroMapper.toShiro(content), false);
        return String.valueOf(required(check(resp).getMessageId(), "messageId"));
    }

    @Override
    public String sendPrivateMsg(String userId, MsgContent content) {
        ActionData<MsgId> resp = bot.sendPrivateMsg(num(userId), ShiroMapper.toShiro(content), false);
        return String.valueOf(required(check(resp).getMessageId(), "messageId"));
    }

    @Override
    public MsgDetail getMsg(String messageId) {
        MsgResp resp = check(bot.getMsg((int) num(messageId)));
        var sender = required(resp.getSender(), "sender");
        return new MsgDetail(
                messageId,
                String.valueOf(required(resp.getUserId(), "userId")),
                required(sender.getNickname(), "nickname"),
                ShiroMapper.fromShiro(required(resp.getArrayMsg(), "arrayMsg"))
        );
    }

    @Override
    public void recall(String messageId) {
        check(bot.deleteMsg((int) num(messageId)));
    }

    // ════ Poke ════════════════════

    @Override
    public void pokeGroup(String groupId, String userId) {
        check(bot.sendGroupPoke(num(groupId), num(userId)));
    }

    @Override
    public void pokePrivate(String userId) {
        check(bot.sendFriendPoke(num(userId)));
    }

    // ════ Mute ════════════════════

    @Override
    public void mute(String groupId, String userId, int seconds) {
        check(bot.setGroupBan(num(groupId), num(userId), seconds));
    }

    // ════ Info queries ════════════

    @Override
    public UserInfo getLoginInfo() {
        var resp = check(bot.getLoginInfo());
        return new UserInfo(
                String.valueOf(required(resp.getUserId(), "userId")),
                required(resp.getNickname(), "nickname")
        );
    }

    @Override
    public UserInfo getUserInfo(String userId) {
        var resp = check(bot.getStrangerInfo(num(userId), true));
        return new UserInfo(
                String.valueOf(required(resp.getUserId(), "userId")),
                required(resp.getNickname(), "nickname")
        );
    }

    @Override
    public GroupInfo getGroupInfo(String groupId) {
        var resp = check(bot.getGroupInfo(num(groupId), true));
        return new GroupInfo(
                groupId,
                required(resp.getGroupName(), "groupName")
        );
    }

    // ════ Helpers ═════════════════

    private static long num(String id) {
        try {
            return Long.parseLong(id);
        } catch (NumberFormatException e) {
            throw new ShiroException("Malformed id: " + id);
        }
    }

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
