package com.zincoid.nullbot.adapter.qq;

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
 * <p>每个入站事件可携带事件上下文；所有输出使用主动消息，避免被动回复被平台自动 @。</p>
 * <p>Each inbound event may carry its context; all output uses active messages to avoid platform-added passive mentions.</p>
 */
@Slf4j
public class QqClient implements BotClient {

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
        StringBuilder text = new StringBuilder();
        String last = null;
        for (MsgSegment segment : content.segments()) {
            switch (segment) {
                case MsgSegment.Text s -> text.append(s.text());
                case MsgSegment.At s -> text.append('@').append(s.userId());
                case MsgSegment.Media m -> {
                    if (!text.isEmpty()) {
                        last = sendText(text.toString(), target);
                        text.setLength(0);
                    }
                    last = index.rememberSent(media.send(m, target), target);
                }
                case MsgSegment.Reply _ -> {
                    // Output is sent as an active message; quoted replies are not used.
                }
                case MsgSegment.Face s -> log.debug("QQ face segment ignored: {}", s.id());
                case MsgSegment.Raw s -> log.debug("QQ raw segment ignored: {}", s.type());
            }
        }
        if (!text.isEmpty()) last = sendText(text.toString(), target);
        if (last == null) throw new QqException("Empty QQ message");
        return last;
    }

    private String sendText(String text, QqTarget target) {
        String last = null;
        for (String chunk : chunks(text)) {
            String actual = api.sendText(target, chunk);
            last = index.rememberSent(actual, target);
        }
        return last;
    }

    // ════ Info ═══════════════════

    @Override
    public UserInfo getLoginInfo() {
        try {
            QqModels.BotInfo info = api.botInfo();
            if (info != null) {
                String id = QqText.isBlank(info.id()) ? selfId : info.id();
                String name = QqText.isBlank(info.username()) ? id : info.username();
                return new UserInfo(id, name, info.avatar());
            }
        } catch (RuntimeException e) {
            log.debug("QQ login info failed", e);
        }
        return new UserInfo(selfId, selfId, null);
    }

    @Override
    public UserInfo getUserInfo(String userId) {
        if (selfId != null && selfId.equals(userId)) return getLoginInfo();
        String name = index.username(userId);
        return new UserInfo(userId, QqText.isBlank(name) ? userId : name, null);
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
                            null))
                    .toList();
        } catch (RuntimeException e) {
            log.debug("QQ group members failed: {}", groupId, e);
            return List.of();
        }
    }

    // ════ Message actions ════════

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
}
