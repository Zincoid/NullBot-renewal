package com.zincoid.nullbot.adapter.qq;

import com.fasterxml.jackson.databind.JsonNode;
import com.zincoid.nullbot.adapter.qq.model.QqModels;
import com.zincoid.nullbot.bot.spi.event.GroupMsgEvent;
import com.zincoid.nullbot.bot.spi.event.PrivateMsgEvent;
import com.zincoid.nullbot.bot.spi.model.MsgDetail;
import com.zincoid.nullbot.bot.spi.port.BotServer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * QQ 官方入站事件路由。
 * <p>QQ official inbound event routing.</p>
 * <p>把群聊与 C2C 事件转换为 SPI 事件，写入消息索引后交给 {@link BotServer}。</p>
 * <p>Converts group and C2C events to SPI events, records them in the message index, and passes them to {@link BotServer}.</p>
 */
@Component
@RequiredArgsConstructor
public class QqInbound {

    private final QqHttpClient http;
    private final QqMessageIndex index;
    private final QqClientFactory clients;
    private final BotServer server;

    /**
     * 处理群消息。
     * <p>Handles a group message.</p>
     *
     * @param node    the event data
     * @param selfId  the bot id
     * @param atEvent whether the event is a dedicated @-bot event
     */
    public void handleGroup(JsonNode node, String selfId, boolean atEvent) {
        if (QqText.isBlank(selfId)) return;
        QqModels.GroupMessage data = http.read(node, QqModels.GroupMessage.class);
        if (data == null || QqText.isBlank(data.id()) || QqText.isBlank(data.groupOpenid())) return;
        if (skip(data.author(), selfId)) return;
        String shortId = QqIds.shortId(data.id());
        QqTarget target = new QqTarget(true, data.groupOpenid());
        GroupMsgEvent event = atEvent
                ? QqMapper.groupAt(data, selfId, shortId)
                : QqMapper.groupFull(data, selfId, shortId);
        if (event == null) return;
        rememberNames(data.author(), data.mentions());
        MsgDetail detail = new MsgDetail(shortId, event.userId(), event.nickname(), event.content());
        if (!index.remember(data.id(), target, detail)) return;
        server.handle(clients.create(selfId, target), event);
    }

    /**
     * 处理 C2C 私聊消息。
     * <p>Handles a C2C private message.</p>
     *
     * @param node   the event data
     * @param selfId the bot id
     */
    public void handleC2c(JsonNode node, String selfId) {
        if (QqText.isBlank(selfId)) return;
        QqModels.C2cMessage data = http.read(node, QqModels.C2cMessage.class);
        if (data == null || QqText.isBlank(data.id()) || data.author() == null) return;
        if (skip(data.author(), selfId)) return;
        String userId = QqText.firstNonBlank(data.author().userOpenid(), data.author().id());
        if (QqText.isBlank(userId)) return;
        String shortId = QqIds.shortId(data.id());
        QqTarget target = new QqTarget(false, userId);
        PrivateMsgEvent event = QqMapper.c2c(data, selfId, shortId);
        if (event == null) return;
        rememberNames(data.author(), data.mentions());
        MsgDetail detail = new MsgDetail(shortId, event.userId(), event.nickname(), event.content());
        if (!index.remember(data.id(), target, detail)) return;
        server.handle(clients.create(selfId, target), event);
    }

    private boolean skip(QqModels.Author author, String selfId) {
        if (author == null || Boolean.TRUE.equals(author.bot())) return true;
        return selfId != null
                && (selfId.equals(author.id())
                || selfId.equals(author.memberOpenid())
                || selfId.equals(author.userOpenid()));
    }

    private void rememberNames(QqModels.Author author, List<QqModels.Mention> mentions) {
        if (author != null) {
            index.rememberUser(author.id(), author.username());
            index.rememberUser(author.memberOpenid(), author.username());
            index.rememberUser(author.userOpenid(), author.username());
        }
        if (mentions != null) {
            for (QqModels.Mention mention : mentions) {
                if (mention == null) continue;
                index.rememberUser(mention.id(), mention.username());
                index.rememberUser(mention.memberOpenid(), mention.username());
                index.rememberUser(mention.userOpenid(), mention.username());
            }
        }
    }

}
