package com.zincoid.nullbot.adapter.qq;

import com.zincoid.nullbot.adapter.qq.api.QqApi;
import com.zincoid.nullbot.adapter.qq.api.QqMedia;
import com.zincoid.nullbot.adapter.qq.model.QqModels;
import com.zincoid.nullbot.bot.spi.model.GroupInfo;
import com.zincoid.nullbot.bot.spi.model.MsgContent;
import com.zincoid.nullbot.bot.spi.model.MsgDetail;
import com.zincoid.nullbot.bot.spi.model.MsgSegment;
import com.zincoid.nullbot.bot.spi.model.UserInfo;
import com.zincoid.nullbot.bot.spi.port.BotClient;
import com.zincoid.nullbot.bot.spi.port.SpiException;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * QQ 官方机器人出站端口。
 * <p>QQ official bot outbound port.</p>
 * <p>每个入站事件可携带事件上下文；所有输出使用主动消息，避免被动回复被平台自动 @；群聊提及使用 Markdown 标签。</p>
 * <p>Each inbound event may carry its context; all output uses active messages to avoid platform-added passive mentions; group mentions use Markdown tags.</p>
 */
@Slf4j
public class QqClient implements BotClient {

    private static final String AVATAR_URL = "https://q.qlogo.cn/qqapp/%s/%s/640";

    private final QqApi api;
    private final QqMessageIndex index;
    private final QqProperties properties;
    private final String selfId;
    private final QqTarget eventTarget;
    private final QqMedia media;

    /**
     * 创建出站端口。
     * <p>Creates the outbound port.</p>
     *
     * @param api         the API client
     * @param index       the message index
     * @param properties  the adapter properties
     * @param selfId      the bot id
     * @param eventTarget the inbound event target, nullable for the lifecycle client
     * @param media       the media sender
     */
    public QqClient(QqApi api, QqMessageIndex index, QqProperties properties,
                    String selfId, QqTarget eventTarget, QqMedia media) {
        this.api = api;
        this.index = index;
        this.properties = properties;
        this.selfId = selfId;
        this.eventTarget = eventTarget;
        this.media = media;
    }

    // ════ Messaging ═══════════════

    @Override
    public String sendGroupMsg(String groupId, MsgContent content) {
        return send(content, new QqTarget(true, groupId));
    }

    @Override
    public String sendPrivateMsg(String userId, MsgContent content) {
        return send(content, new QqTarget(false, userId));
    }

    private synchronized String send(MsgContent content, QqTarget target) {
        if (content == null) throw new QqException("Missing QQ message content");
        RichText text = new RichText(target.group());
        String replyId = replyId(content);
        String reference = index.sceneIndex(replyId);
        String last = null;
        for (MsgSegment segment : content.segments()) {
            switch (segment) {
                case MsgSegment.Text s -> text.text(s.text());
                case MsgSegment.At s -> text.at(s.userId());
                case MsgSegment.Media m -> {
                    if (!text.isEmpty()) {
                        last = sendText(text, target, reference);
                        text.clear();
                    }
                    last = index.rememberSent(media.send(m, target, reference), target);
                }
                case MsgSegment.Reply _ -> {
                    // The reference is resolved before the segment loop.
                }
                case MsgSegment.Face s -> log.debug("QQ face segment ignored: {}", s.id());
                case MsgSegment.Raw s -> log.debug("QQ raw segment ignored: {}", s.type());
            }
        }
        if (!text.isEmpty()) last = sendText(text, target, reference);
        if (last == null) throw new QqException("Empty QQ message");
        return last;
    }

    private String sendText(RichText text, QqTarget target, String reference) {
        String last = null;
        if (target.group() && text.hasAt()) {
            try {
                for (String chunk : markdownChunks(text.markdown())) {
                    QqModels.SendResponse response = api.sendMarkdownResult(target, chunk, reference);
                    last = index.rememberSent(response.id(), target, refIdx(response));
                }
                return last;
            } catch (QqException e) {
                if (last != null) throw e;
                log.warn("QQ markdown mention failed, falling back to text: {}", e.getMessage());
            }
        }
        for (String chunk : chunks(text.plain())) {
            QqModels.SendResponse response = api.sendTextResult(target, chunk, reference);
            last = index.rememberSent(response.id(), target, refIdx(response));
        }
        return last;
    }

    private static String replyId(MsgContent content) {
        for (MsgSegment segment : content.segments())
            if (segment instanceof MsgSegment.Reply reply) return reply.messageId();
        return null;
    }

    private static String refIdx(QqModels.SendResponse response) {
        return response == null || response.extInfo() == null
                ? null : response.extInfo().refIdx();
    }

    // ════ Files ═══════════════════

    @Override
    public void sendGroupFile(String groupId, String url, String name) {
        media.sendFile(new QqTarget(true, groupId), url, name);
    }

    @Override
    public void sendPrivateFile(String userId, String url, String name) {
        media.sendFile(new QqTarget(false, userId), url, name);
    }

    // ════ Info ════════════════════

    @Override
    public UserInfo getLoginInfo() {
        try {
            QqModels.BotInfo info = api.botInfo();
            if (info != null) {
                String id = QqText.isBlank(info.id()) ? selfId : info.id();
                String name = QqText.isBlank(info.username()) ? id : info.username();
                String avatar = QqText.isBlank(info.avatar()) ? avatar(id) : info.avatar();
                return new UserInfo(id, name, avatar);
            }
        } catch (RuntimeException e) {
            log.debug("QQ login info failed", e);
        }
        return new UserInfo(selfId, selfId, avatar(selfId));
    }

    @Override
    public UserInfo getUserInfo(String userId) {
        if (selfId != null && selfId.equals(userId)) return getLoginInfo();
        String name = index.username(userId);
        return new UserInfo(userId, QqText.isBlank(name) ? userId : name, avatar(userId));
    }

    @Override
    public GroupInfo getGroupInfo(String groupId) {
        try {
            return new GroupInfo(groupId, api.groupName(groupId));
        } catch (RuntimeException e) {
            log.debug("QQ group info failed: {}", groupId, e);
            return new GroupInfo(groupId, groupId);
        }
    }

    @Override
    public List<UserInfo> getGroupMembers(String groupId) {
        try {
            return api.groupMembers(groupId).stream()
                    .filter(member -> member != null && !QqText.isBlank(member.memberOpenid()))
                    .filter(member -> !Boolean.TRUE.equals(member.bot()))
                    .map(member -> new UserInfo(
                            member.memberOpenid(),
                            QqText.isBlank(member.username())
                                    ? member.memberOpenid() : member.username(),
                            avatar(member.memberOpenid())))
                    .toList();
        } catch (RuntimeException e) {
            log.debug("QQ group members failed: {}", groupId, e);
            return List.of();
        }
    }

    // ════ Message actions ═════════

    @Override
    public MsgDetail getMsg(String messageId) {
        QqMessageIndex.MessageEntry entry = index.resolve(messageId);
        if (entry == null || entry.detail() == null)
            throw SpiException.unsupported("getMsg");
        return entry.detail();
    }

    @Override
    public void recall(String messageId) {
        QqMessageIndex.MessageEntry entry = index.resolve(messageId);
        QqTarget target = entry == null ? eventTarget : entry.target();
        if (target == null) throw new QqException("Cannot resolve QQ message target");
        api.recall(target, entry == null ? messageId : entry.actualId());
    }

    @Override
    public void mute(String groupId, String userId, int seconds) {
        api.mute(groupId, userId, seconds);
    }

    // ════ Helpers ═════════════════

    private String avatar(String openId) {
        String appId = properties.getAppId();
        if (QqText.isBlank(appId) || QqText.isBlank(openId)) return null;
        return AVATAR_URL.formatted(appId, openId);
    }

    private List<String> chunks(String text) {
        int limit = Math.max(100, properties.getTextLimit());
        if (text.codePointCount(0, text.length()) <= limit) return List.of(text);
        List<String> chunks = new ArrayList<>();
        int start = 0;
        int count = 0;
        for (int i = 0; i < text.length(); ) {
            int codePoint = text.codePointAt(i);
            if (count >= limit) {
                chunks.add(text.substring(start, i));
                start = i;
                count = 0;
            }
            count++;
            i += Character.charCount(codePoint);
        }
        if (start < text.length()) chunks.add(text.substring(start));
        return chunks;
    }

    private List<String> markdownChunks(String text) {
        int limit = Math.max(100, properties.getTextLimit());
        if (text.codePointCount(0, text.length()) <= limit) return List.of(text);
        List<String> chunks = new ArrayList<>();
        int start = 0;
        int count = 0;
        boolean inTag = false;
        for (int i = 0; i < text.length(); ) {
            char current = text.charAt(i);
            if (current == '<' && text.startsWith("<qqbot-", i)) inTag = true;
            if (count >= limit && !inTag) {
                chunks.add(text.substring(start, i));
                start = i;
                count = 0;
            }
            int codePoint = text.codePointAt(i);
            if (current == '>') inTag = false;
            count++;
            i += Character.charCount(codePoint);
        }
        if (start < text.length()) chunks.add(text.substring(start));
        return chunks;
    }

    /**
     * 待发送文本：同时维护纯文本与 Markdown 提及形式。
     * <p>Pending text that keeps both plain-text and Markdown mention forms.</p>
     */
    private static final class RichText {

        private final boolean group;
        private final StringBuilder plain = new StringBuilder();
        private final StringBuilder markdown = new StringBuilder();
        private boolean hasAt;
        private boolean lastAt;

        private RichText(boolean group) {
            this.group = group;
        }

        private void text(String value) {
            if (value == null || value.isEmpty()) return;
            plain.append(value);
            String markdownValue = value;
            if (lastAt && markdownValue.charAt(0) == ' ')
                markdownValue = markdownValue.substring(1);
            markdown.append(markdownValue);
            lastAt = false;
        }

        private void at(String userId) {
            if (QqText.isBlank(userId)) return;
            String id = userId.strip();
            plain.append('@').append(id);
            if (!group) return;
            hasAt = true;
            if ("all".equals(id)) {
                markdown.append("<qqbot-at-everyone /> ");
            } else {
                markdown.append("<qqbot-at-user id=\"").append(id).append("\" /> ");
            }
            lastAt = true;
        }

        private boolean isEmpty() {
            return plain.length() == 0;
        }

        private boolean hasAt() {
            return hasAt;
        }

        private String plain() {
            return plain.toString();
        }

        private String markdown() {
            return markdown.toString();
        }

        private void clear() {
            plain.setLength(0);
            markdown.setLength(0);
            hasAt = false;
            lastAt = false;
        }
    }
}
