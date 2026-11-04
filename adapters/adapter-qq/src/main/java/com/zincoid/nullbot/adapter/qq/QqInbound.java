package com.zincoid.nullbot.adapter.qq;

import com.fasterxml.jackson.databind.JsonNode;
import com.zincoid.nullbot.adapter.qq.model.QqModels;
import com.zincoid.nullbot.bot.spi.event.GroupMsgEvent;
import com.zincoid.nullbot.bot.spi.event.PrivateMsgEvent;
import com.zincoid.nullbot.bot.spi.model.MsgContent;
import com.zincoid.nullbot.bot.spi.model.MsgDetail;
import com.zincoid.nullbot.bot.spi.port.BotServer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
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
        Quoted quote = quoted(data.messageScene(), data.msgElements(), data.messageType(),
                data.id(), data.author(), selfId);
        if (quote != null) index.rememberQuoted(quote.actualId(), target, quote.detail());
        GroupMsgEvent event = atEvent
                ? QqMapper.groupAt(data, selfId, shortId, quote == null ? null : quote.shortId())
                : QqMapper.groupFull(data, selfId, shortId, quote == null ? null : quote.shortId());
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
        Quoted quote = quoted(data.messageScene(), data.msgElements(), data.messageType(),
                data.id(), data.author(), selfId);
        if (quote != null) index.rememberQuoted(quote.actualId(), target, quote.detail());
        PrivateMsgEvent event = QqMapper.c2c(data, selfId, shortId,
                quote == null ? null : quote.shortId());
        if (event == null) return;
        rememberNames(data.author(), data.mentions());
        MsgDetail detail = new MsgDetail(shortId, event.userId(), event.nickname(), event.content());
        if (!index.remember(data.id(), target, detail)) return;
        server.handle(clients.create(selfId, target), event);
    }

    private record Quoted(String shortId, String actualId, MsgDetail detail) {
    }

    private Quoted quoted(JsonNode scene, JsonNode elements, Integer messageType,
                          String currentId, QqModels.Author currentAuthor, String selfId) {
        String ref = refIndex(scene);
        if (ref == null && !Integer.valueOf(103).equals(messageType)) return null;
        if (elements == null || !elements.isArray() || elements.size() == 0) return null;
        JsonNode element = pick(elements, ref);
        if (element == null) return null;
        element = unwrap(element, ref);
        String elementIdx = element.path("msg_idx").asText(null);
        String actualId = QqText.isBlank(ref)
                ? (QqText.isBlank(elementIdx) ? currentId + ":quote" : elementIdx)
                : ref;
        String text = element.path("content").asText("");
        List<QqModels.Attachment> attachments = attachmentsOf(element.get("attachments"));
        MsgContent content = QqMapper.quotedContent(text, attachments, selfId);
        if (content.segments().isEmpty()) return null;
        QqModels.Author author = http.read(element.get("author"), QqModels.Author.class);
        if (author == null) author = currentAuthor;
        String userId = author == null ? "" : QqText.firstNonBlank(
                author.memberOpenid(), QqText.firstNonBlank(author.userOpenid(), author.id()));
        if (QqText.isBlank(userId)) userId = "quoted";
        String nickname = author == null ? userId : QqText.firstNonBlank(author.username(), userId);
        String shortId = QqIds.shortId(actualId);
        if (author != null) index.rememberUser(userId, nickname);
        return new Quoted(shortId, actualId, new MsgDetail(shortId, userId, nickname, content));
    }

    private static String refIndex(JsonNode scene) {
        if (scene == null) return null;
        JsonNode direct = scene.get("ref_msg_idx");
        if (direct != null && direct.isTextual()) return direct.asText();
        JsonNode ext = scene.get("ext");
        if (ext == null || !ext.isArray()) return null;
        for (JsonNode item : ext) {
            String value = item.asText("");
            if (value.startsWith("ref_msg_idx="))
                return value.substring("ref_msg_idx=".length());
        }
        return null;
    }

    private static JsonNode pick(JsonNode elements, String ref) {
        JsonNode first = null;
        for (JsonNode element : elements) {
            if (first == null) first = element;
            if (!QqText.isBlank(ref) && ref.equals(element.path("msg_idx").asText(null)))
                return element;
        }
        return first;
    }

    private static JsonNode unwrap(JsonNode element, String ref) {
        for (int depth = 0; depth < 4 && !hasContent(element); depth++) {
            JsonNode nested = element.get("msg_elements");
            if (nested == null || !nested.isArray() || nested.size() == 0) break;
            JsonNode child = pick(nested, ref);
            if (child == null) break;
            element = child;
        }
        return element;
    }

    private static boolean hasContent(JsonNode element) {
        if (element == null) return false;
        if (!element.path("content").asText("").isBlank()) return true;
        JsonNode attachments = element.get("attachments");
        return attachments != null && attachments.isArray() && attachments.size() > 0;
    }

    private List<QqModels.Attachment> attachmentsOf(JsonNode node) {
        if (node == null || !node.isArray()) return List.of();
        List<QqModels.Attachment> list = new ArrayList<>();
        for (JsonNode item : node) {
            QqModels.Attachment attachment = http.read(item, QqModels.Attachment.class);
            if (attachment != null) list.add(attachment);
        }
        return list;
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
