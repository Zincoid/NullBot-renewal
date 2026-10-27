package com.zincoid.nullbot.adapter.qq;

import com.zincoid.nullbot.adapter.qq.model.QqModels;
import com.zincoid.nullbot.bot.spi.event.GroupMsgEvent;
import com.zincoid.nullbot.bot.spi.event.PrivateMsgEvent;
import com.zincoid.nullbot.bot.spi.model.MsgContent;
import com.zincoid.nullbot.bot.spi.model.MsgSegment;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * QQ 官方事件与 SPI 事件之间的转换。
 * <p>Converts QQ official events to and from SPI events.</p>
 */
public final class QqMapper {

    private static final Pattern MENTION = Pattern.compile("<@!?([^>]+)>");

    private QqMapper() {
    }

    /**
     * 转换群内 @机器人 消息。
     * <p>Maps a group @-bot message.</p>
     *
     * @param data    the group message data
     * @param selfId  the bot id
     * @param shortId the internal short message id
     * @return the group message event, or null when the event is empty
     */
    public static GroupMsgEvent groupAt(QqModels.GroupMessage data, String selfId, String shortId) {
        return group(data, selfId, shortId, true, Set.of());
    }

    /**
     * 转换全量群消息。
     * <p>Maps a full group message.</p>
     *
     * @param data    the group message data
     * @param selfId  the bot id
     * @param shortId the internal short message id
     * @return the group message event, or null when the event is empty
     */
    public static GroupMsgEvent groupFull(QqModels.GroupMessage data, String selfId, String shortId) {
        Set<String> botIds = botMentionIds(data, selfId);
        boolean at = !botIds.isEmpty();
        return group(data, selfId, shortId, at, botIds);
    }

    /**
     * 转换 C2C 私聊消息。
     * <p>Maps a C2C private message.</p>
     *
     * @param data    the C2C message data
     * @param selfId  the bot id
     * @param shortId the internal short message id
     * @return the private message event, or null when the event is empty
     */
    public static PrivateMsgEvent c2c(QqModels.C2cMessage data, String selfId, String shortId) {
        if (data == null || QqText.isBlank(data.id()) || QqText.isBlank(shortId) || QqText.isBlank(selfId)) return null;
        QqModels.Author author = data.author();
        if (author == null) return null;
        String userId = QqText.firstNonBlank(author.userOpenid(), author.id());
        if (QqText.isBlank(userId)) return null;
        String nickname = QqText.firstNonBlank(author.username(), userId);
        MsgContent content = content(data.content(), data.attachments(), Set.of(), selfId);
        if (content.segments().isEmpty()) return null;
        return new PrivateMsgEvent(selfId, userId, shortId, nickname, content, false, false);
    }

    // ════ Helpers ═════════════════

    private static GroupMsgEvent group(QqModels.GroupMessage data, String selfId, String shortId,
                                       boolean at, Set<String> botIds) {
        if (data == null || QqText.isBlank(data.id()) || QqText.isBlank(data.groupOpenid())
                || QqText.isBlank(shortId) || QqText.isBlank(selfId)) return null;
        QqModels.Author author = data.author();
        if (author == null) return null;
        String userId = QqText.firstNonBlank(author.memberOpenid(), author.id());
        if (QqText.isBlank(userId)) return null;
        String nickname = QqText.firstNonBlank(author.username(), userId);
        MsgContent content = content(data.content(), data.attachments(), botIds, selfId);
        if (content.segments().isEmpty()) return null;
        return new GroupMsgEvent(
                selfId, userId, data.groupOpenid(), shortId, nickname, content, at, false);
    }

    private static MsgContent content(String text, List<QqModels.Attachment> attachments,
                                      Set<String> botIds, String selfId) {
        List<MsgSegment> segments = new ArrayList<>();
        if (text != null && !text.isEmpty()) {
            Matcher matcher = MENTION.matcher(text);
            int last = 0;
            while (matcher.find()) {
                addText(segments, text.substring(last, matcher.start()));
                String id = matcher.group(1);
                if (!"all".equals(id) && (botIds.contains(id) || id.equals(selfId))) {
                    // Drop the bot's own mention token.
                } else {
                    segments.add(new MsgSegment.At(id));
                }
                last = matcher.end();
            }
            addText(segments, text.substring(last));
        }
        if (attachments != null) {
            for (QqModels.Attachment attachment : attachments) {
                MsgSegment segment = attachment(attachment);
                if (segment != null) segments.add(segment);
            }
        }
        trimEdges(segments);
        return MsgContent.of(segments);
    }

    private static void addText(List<MsgSegment> segments, String text) {
        if (text == null || text.isEmpty()) return;
        String value = text;
        if (segments.isEmpty() || segments.getLast() instanceof MsgSegment.At) value = value.stripLeading();
        if (!value.isEmpty()) segments.add(new MsgSegment.Text(value));
    }

    private static MsgSegment attachment(QqModels.Attachment attachment) {
        if (attachment == null) return null;
        String url = QqText.isBlank(attachment.url()) ? attachment.voiceWavUrl() : attachment.url();
        if (QqText.isBlank(url)) return null;
        String type = attachment.contentType() == null
                ? "" : attachment.contentType().toLowerCase(Locale.ROOT);
        String filename = attachment.filename();
        if (type.startsWith("image/")) return new MsgSegment.Image(url, filename);
        if (type.startsWith("video/")) return new MsgSegment.Video(url, filename);
        if ("voice".equals(type) || type.startsWith("audio/")) return new MsgSegment.Voice(url, filename);
        return new MsgSegment.Raw("file", url);
    }

    private static void trimEdges(List<MsgSegment> segments) {
        while (!segments.isEmpty() && segments.getFirst() instanceof MsgSegment.Text(String text)) {
            String value = text.stripLeading();
            if (value.isEmpty()) segments.removeFirst();
            else {
                segments.set(0, new MsgSegment.Text(value));
                break;
            }
        }
        while (!segments.isEmpty() && segments.getLast() instanceof MsgSegment.Text(String text)) {
            String value = text.stripTrailing();
            if (value.isEmpty()) segments.removeLast();
            else {
                segments.set(segments.size() - 1, new MsgSegment.Text(value));
                break;
            }
        }
    }

    private static Set<String> botMentionIds(QqModels.GroupMessage data, String selfId) {
        Set<String> ids = new HashSet<>();
        if (data == null) return ids;
        if (data.mentions() != null) {
            for (QqModels.Mention mention : data.mentions()) {
                if (mention == null) continue;
                if (Boolean.TRUE.equals(mention.isYou()) || matches(mention, selfId)) {
                    addId(ids, mention.id());
                    addId(ids, mention.memberOpenid());
                    addId(ids, mention.userOpenid());
                }
            }
        }
        if (selfId != null && data.content() != null
                && (data.content().contains("<@" + selfId + ">")
                || data.content().contains("<@!" + selfId + ">"))) {
            ids.add(selfId);
        }
        return ids;
    }

    private static boolean matches(QqModels.Mention mention, String selfId) {
        return selfId != null
                && (selfId.equals(mention.id())
                || selfId.equals(mention.memberOpenid())
                || selfId.equals(mention.userOpenid()));
    }

    private static void addId(Set<String> ids, String id) {
        if (!QqText.isBlank(id)) ids.add(id);
    }
}
