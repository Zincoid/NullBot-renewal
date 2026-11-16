package com.zincoid.nullbot.adapter.qq;

import com.fasterxml.jackson.databind.JsonNode;
import com.zincoid.nullbot.adapter.qq.api.QqHttpClient;
import com.zincoid.nullbot.adapter.qq.model.QqModels;
import com.zincoid.nullbot.bot.spi.event.GroupMsgEvent;
import com.zincoid.nullbot.bot.spi.event.JoinEvent;
import com.zincoid.nullbot.bot.spi.event.LeaveEvent;
import com.zincoid.nullbot.bot.spi.event.MsgEvent;
import com.zincoid.nullbot.bot.spi.event.PrivateMsgEvent;
import com.zincoid.nullbot.bot.spi.model.MsgDetail;
import com.zincoid.nullbot.bot.spi.port.BotServer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * QQ 官方入站事件路由。
 * <p>QQ official inbound event routing.</p>
 * <p>把群聊、C2C 与群成员事件转换为 SPI 事件，写入消息索引后交给 {@link BotServer}。</p>
 * <p>Converts group, C2C, and group member events to SPI events, records them in the message index, and passes them to {@link BotServer}.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class QqInbound {

    private final QqHttpClient http;
    private final QqMessageIndex index;
    private final QqClientFactory clients;
    private final BotServer server;
    private final QqQuoteResolver quotes;

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
        QqQuote quote = resolveQuote(data.messageScene(), data.msgElements(), data.messageType(),
                data.id(), target, selfId);
        GroupMsgEvent event = atEvent
                ? QqMapper.groupAt(data, selfId, shortId, quote == null ? null : quote.shortId())
                : QqMapper.groupFull(data, selfId, shortId, quote == null ? null : quote.shortId());
        if (event == null) return;
        rememberNames(data.author(), data.mentions());
        publish(selfId, target, data.id(), shortId, data.messageScene(), event);
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
        QqQuote quote = resolveQuote(data.messageScene(), data.msgElements(), data.messageType(),
                data.id(), target, selfId);
        PrivateMsgEvent event = QqMapper.c2c(data, selfId, shortId,
                quote == null ? null : quote.shortId());
        if (event == null) return;
        rememberNames(data.author(), data.mentions());
        publish(selfId, target, data.id(), shortId, data.messageScene(), event);
    }

    /**
     * 处理群成员加入事件。
     * <p>Handles a group member join event.</p>
     *
     * @param node   the event data
     * @param selfId the bot id
     */
    public void handleMemberAdd(JsonNode node, String selfId) {
        QqModels.GroupMember data = member(node, selfId);
        if (data == null) return;
        server.handle(clients.create(selfId, new QqTarget(true, data.groupOpenid())),
                new JoinEvent(selfId, data.groupOpenid(), data.memberOpenid(),
                        data.memberOpenid()));
    }

    /**
     * 处理群成员退出事件。
     * <p>Handles a group member leave event.</p>
     *
     * @param node   the event data
     * @param selfId the bot id
     */
    public void handleMemberRemove(JsonNode node, String selfId) {
        QqModels.GroupMember data = member(node, selfId);
        if (data == null) return;
        server.handle(clients.create(selfId, new QqTarget(true, data.groupOpenid())),
                new LeaveEvent(selfId, data.groupOpenid(), data.memberOpenid(),
                        data.memberOpenid(), false));
    }

    private void publish(String selfId, QqTarget target, String actualId, String shortId,
                         JsonNode scene, MsgEvent event) {
        MsgDetail detail = new MsgDetail(shortId, event.userId(), event.nickname(), event.content());
        index.rememberIndex(QqScenes.selfIndex(scene), shortId);
        if (!index.remember(actualId, target, detail)) return;
        server.handle(clients.create(selfId, target), event);
    }

    private QqModels.GroupMember member(JsonNode node, String selfId) {
        if (QqText.isBlank(selfId)) return null;
        QqModels.GroupMember data = http.read(node, QqModels.GroupMember.class);
        if (data == null || QqText.isBlank(data.groupOpenid())
                || QqText.isBlank(data.memberOpenid())) return null;
        return data;
    }

    private QqQuote resolveQuote(JsonNode scene, JsonNode elements, Integer messageType,
                                 String currentId, QqTarget target, String selfId) {
        try {
            return quotes.resolve(scene, elements, messageType, currentId, target, selfId);
        } catch (RuntimeException e) {
            log.warn("QQ quote resolve failed: {}", e.getMessage());
            return null;
        }
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
