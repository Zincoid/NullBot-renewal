package com.zincoid.nullbot.adapter.shiro;

import com.mikuac.shiro.annotation.GroupDecreaseHandler;
import com.mikuac.shiro.annotation.GroupIncreaseHandler;
import com.mikuac.shiro.annotation.GroupMessageHandler;
import com.mikuac.shiro.annotation.GroupMsgDeleteNoticeHandler;
import com.mikuac.shiro.annotation.GroupPokeNoticeHandler;
import com.mikuac.shiro.annotation.PrivateMessageHandler;
import com.mikuac.shiro.annotation.PrivateMsgDeleteNoticeHandler;
import com.mikuac.shiro.annotation.PrivatePokeNoticeHandler;
import com.mikuac.shiro.annotation.common.Shiro;
import com.mikuac.shiro.core.Bot;
import com.mikuac.shiro.dto.event.message.GroupMessageEvent;
import com.mikuac.shiro.dto.event.message.PrivateMessageEvent;
import com.mikuac.shiro.dto.event.notice.GroupDecreaseNoticeEvent;
import com.mikuac.shiro.dto.event.notice.GroupIncreaseNoticeEvent;
import com.mikuac.shiro.dto.event.notice.GroupMsgDeleteNoticeEvent;
import com.mikuac.shiro.dto.event.notice.PokeNoticeEvent;
import com.mikuac.shiro.dto.event.notice.PrivateMsgDeleteNoticeEvent;
import com.zincoid.nullbot.bot.spi.event.BotEvent;
import com.zincoid.nullbot.bot.spi.event.GroupMsgEvent;
import com.zincoid.nullbot.bot.spi.event.GroupPokeEvent;
import com.zincoid.nullbot.bot.spi.event.GroupRecallEvent;
import com.zincoid.nullbot.bot.spi.event.JoinEvent;
import com.zincoid.nullbot.bot.spi.event.LeaveEvent;
import com.zincoid.nullbot.bot.spi.event.PrivateMsgEvent;
import com.zincoid.nullbot.bot.spi.event.PrivatePokeEvent;
import com.zincoid.nullbot.bot.spi.event.PrivateRecallEvent;
import com.zincoid.nullbot.bot.spi.model.MsgContent;
import com.zincoid.nullbot.bot.spi.model.MsgSegment;
import com.zincoid.nullbot.bot.spi.port.BotServer;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Shiro 入站适配器：归一化框架事件为 SPI 事件并交给 {@link BotServer}。
 * <p>Shiro inbound adapter: normalizes framework events into SPI events and hands them to {@link BotServer}.</p>
 */
@Shiro
@Component
@ConditionalOnProperty(name = "bot.adapter", havingValue = "shiro", matchIfMissing = true)
@RequiredArgsConstructor
public class ShiroListener {

    private final BotServer server;

    // ════ Messaging ═══════════════

    @GroupMessageHandler
    public void onGroupMsg(Bot bot, GroupMessageEvent event) {
        MsgContent content = ShiroMapper.fromShiro(
                required(event.getArrayMsg(), "arrayMsg"));
        String selfId = String.valueOf(bot.getSelfId());
        var sender = required(event.getSender(), "sender");
        deliver(bot, new GroupMsgEvent(
                selfId,
                String.valueOf(required(event.getUserId(), "userId")),
                String.valueOf(required(event.getGroupId(), "groupId")),
                String.valueOf(required(event.getMessageId(), "messageId")),
                required(sender.getNickname(), "nickname"),
                content,
                at(content, selfId),
                reply(content)
        ));
    }

    @PrivateMessageHandler
    public void onPrivateMsg(Bot bot, PrivateMessageEvent event) {
        MsgContent content = ShiroMapper.fromShiro(
                required(event.getArrayMsg(), "arrayMsg"));
        String selfId = String.valueOf(bot.getSelfId());
        var sender = required(event.getPrivateSender(), "sender");
        deliver(bot, new PrivateMsgEvent(
                selfId,
                String.valueOf(required(event.getUserId(), "userId")),
                String.valueOf(required(event.getMessageId(), "messageId")),
                required(sender.getNickname(), "nickname"),
                content,
                at(content, selfId),
                reply(content)
        ));
    }

    // ════ Poke ════════════════════

    @GroupPokeNoticeHandler
    public void onGroupPoke(Bot bot, PokeNoticeEvent event) {
        deliver(bot, new GroupPokeEvent(
                String.valueOf(bot.getSelfId()),
                String.valueOf(required(event.getUserId(), "userId")),
                String.valueOf(required(event.getTargetId(), "targetId")),
                String.valueOf(required(event.getGroupId(), "groupId"))
        ));
    }

    @PrivatePokeNoticeHandler
    public void onPrivatePoke(Bot bot, PokeNoticeEvent event) {
        deliver(bot, new PrivatePokeEvent(
                String.valueOf(bot.getSelfId()),
                String.valueOf(required(event.getUserId(), "userId")),
                String.valueOf(required(event.getTargetId(), "targetId"))
        ));
    }

    // ════ Recall ══════════════════

    @GroupMsgDeleteNoticeHandler
    public void onGroupRecall(Bot bot, GroupMsgDeleteNoticeEvent event) {
        deliver(bot, new GroupRecallEvent(
                String.valueOf(bot.getSelfId()),
                String.valueOf(required(event.getUserId(), "userId")),
                String.valueOf(required(event.getMessageId(), "messageId")),
                String.valueOf(required(event.getGroupId(), "groupId")),
                String.valueOf(required(event.getOperatorId(), "operatorId"))
        ));
    }

    @PrivateMsgDeleteNoticeHandler
    public void onPrivateRecall(Bot bot, PrivateMsgDeleteNoticeEvent event) {
        deliver(bot, new PrivateRecallEvent(
                String.valueOf(bot.getSelfId()),
                String.valueOf(required(event.getUserId(), "userId")),
                String.valueOf(required(event.getMessageId(), "messageId"))
        ));
    }

    // ════ Group membership ════════

    @GroupIncreaseHandler
    public void onJoin(Bot bot, GroupIncreaseNoticeEvent event) {
        deliver(bot, new JoinEvent(
                String.valueOf(bot.getSelfId()),
                String.valueOf(required(event.getGroupId(), "groupId")),
                String.valueOf(required(event.getUserId(), "userId")),
                String.valueOf(required(event.getOperatorId(), "operatorId"))
        ));
    }

    @GroupDecreaseHandler
    public void onLeave(Bot bot, GroupDecreaseNoticeEvent event) {
        deliver(bot, new LeaveEvent(
                String.valueOf(bot.getSelfId()),
                String.valueOf(required(event.getGroupId(), "groupId")),
                String.valueOf(required(event.getUserId(), "userId")),
                String.valueOf(required(event.getOperatorId(), "operatorId")),
                "kick".equals(event.getSubType())
        ));
    }

    // ════ Helpers ═════════════════


    private static <T> T required(T value, String name) {
        if (value == null) throw new ShiroException("Missing value: " + name);
        return value;
    }

    private void deliver(Bot bot, BotEvent event) {
        server.handle(new ShiroClient(bot), event);
    }

    private static boolean at(MsgContent content, String selfId) {
        return content.segments().stream()
                .filter(MsgSegment.At.class::isInstance)
                .map(MsgSegment.At.class::cast)
                .anyMatch(at -> selfId.equals(at.userId()));
    }

    private static boolean reply(MsgContent content) {
        return !content.segments().isEmpty() &&
                content.segments().getFirst() instanceof MsgSegment.Reply;
    }
}
