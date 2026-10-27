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

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * QQ 官方机器人出站端口。
 * <p>QQ official bot outbound port.</p>
 * <p>每个入站事件创建实例并携带被动回复上下文；生命周期实例只用于资料查询。</p>
 * <p>One instance is created per inbound event with its passive-reply context; the lifecycle instance is only for profile queries.</p>
 */
@Slf4j
public class QqClient implements BotClient {

    private static final int MAX_LOCAL_MEDIA_BYTES = 5 * 1024 * 1024;
    private static final int GROUP_PASSIVE_SECONDS = 300;
    private static final int C2C_PASSIVE_SECONDS = 3600;
    private static final int GROUP_PASSIVE_REPLIES = 5;
    private static final int C2C_PASSIVE_REPLIES = 4;

    private final QqApi api;
    private final QqMessageIndex index;
    private final QqProperties properties;
    private final String selfId;
    private final ReplyContext reply;

    private int seq = 1;
    private int sent;

    /**
     * 被动回复上下文。
     * <p>Passive reply context.</p>
     *
     * @param target     the target conversation
     * @param messageId  the platform message id
     * @param receivedAt the local receive time
     */
    public record ReplyContext(QqMessageIndex.Target target, String messageId, Instant receivedAt) {
    }

    /**
     * 创建出站端口。
     * <p>Creates the outbound port.</p>
     *
     * @param api        the API client
     * @param index      the message index
     * @param properties the adapter properties
     * @param selfId     the bot id
     * @param reply      the passive reply context, nullable for the lifecycle client
     */
    public QqClient(QqApi api, QqMessageIndex index, QqProperties properties,
                    String selfId, ReplyContext reply) {
        this.api = api;
        this.index = index;
        this.properties = properties;
        this.selfId = selfId;
        this.reply = reply;
    }

    // ════ Messaging ═══════════════

    @Override
    public String sendGroupMsg(String groupId, MsgContent content) {
        if (reply == null || !reply.target().group() || !reply.target().id().equals(groupId))
            throw activeUnsupported();
        return send(content);
    }

    @Override
    public String sendPrivateMsg(String userId, MsgContent content) {
        if (reply == null || reply.target().group() || !reply.target().id().equals(userId))
            throw activeUnsupported();
        return send(content);
    }

    private synchronized String send(MsgContent content) {
        if (content == null) throw new QqException("Missing QQ message content");
        checkContext();
        StringBuilder text = new StringBuilder();
        String last = null;
        for (MsgSegment segment : content.segments()) {
            switch (segment) {
                case MsgSegment.Text s -> text.append(s.text());
                case MsgSegment.At s -> text.append('@').append(s.userId());
                case MsgSegment.Media media -> {
                    if (!text.isEmpty()) {
                        last = sendText(text.toString());
                        text.setLength(0);
                    }
                    last = sendMedia(media);
                }
                case MsgSegment.Reply _ -> {
                    // Passive msg_id already authorizes the reply.
                }
                case MsgSegment.Face s -> log.debug("QQ face segment ignored: {}", s.id());
                case MsgSegment.Raw s -> log.debug("QQ raw segment ignored: {}", s.type());
            }
        }
        if (!text.isEmpty()) last = sendText(text.toString());
        if (last == null) throw new QqException("Empty QQ message");
        return last;
    }

    private String sendText(String text) {
        String last = null;
        for (String chunk : chunks(text)) {
            int current = nextSeq();
            String actual = reply.target().group()
                    ? api.sendGroupText(reply.target().id(), chunk, reply.messageId(), current)
                    : api.sendUserText(reply.target().id(), chunk, reply.messageId(), current);
            last = index.rememberSent(actual, reply.target());
        }
        return last;
    }

    private String sendMedia(MsgSegment.Media media) {
        String fileInfo = upload(media);
        int current = nextSeq();
        String actual = reply.target().group()
                ? api.sendGroupMedia(reply.target().id(), fileInfo, reply.messageId(), current)
                : api.sendUserMedia(reply.target().id(), fileInfo, reply.messageId(), current);
        return index.rememberSent(actual, reply.target());
    }

    private String upload(MsgSegment.Media media) {
        int fileType = fileType(media);
        String defaultName = switch (fileType) {
            case 1 -> "image.png";
            case 2 -> "video.mp4";
            default -> "voice.silk";
        };
        String url = media.url();
        if (url == null || url.isBlank()) throw new QqException("Missing QQ media url");
        String fileName = fileName(media, defaultName);
        if ((media.file() == null || media.file().isBlank())
                && (url.startsWith("http://") || url.startsWith("https://"))) {
            String fromUrl = url;
            int query = fromUrl.indexOf('?');
            if (query >= 0) fromUrl = fromUrl.substring(0, query);
            int slash = fromUrl.lastIndexOf('/');
            if (slash >= 0 && slash + 1 < fromUrl.length())
                fileName = fileName(fromUrl.substring(slash + 1), defaultName);
        }
        if (url.startsWith("http://") || url.startsWith("https://")) {
            try {
                return uploadUrl(fileType, url, fileName);
            } catch (QqException e) {
                byte[] bytes = api.download(url, MAX_LOCAL_MEDIA_BYTES);
                return uploadBytes(fileType, bytes, fileName);
            }
        }
        if (url.startsWith(MsgContent.BASE64_PREFIX)) {
            byte[] bytes;
            try {
                bytes = Base64.getDecoder().decode(url.substring(MsgContent.BASE64_PREFIX.length()));
            } catch (IllegalArgumentException e) {
                throw new QqException("Malformed QQ base64 media", e);
            }
            return uploadBytes(fileType, bytes, fileName);
        }
        if (url.startsWith("file://")) {
            try {
                Path path = Path.of(URI.create(url));
                if (Files.size(path) > MAX_LOCAL_MEDIA_BYTES)
                    throw new QqException("QQ local media exceeds 5 MB");
                if (media.file() == null || media.file().isBlank())
                    fileName = fileName(path.getFileName().toString(), defaultName);
                return uploadBytes(fileType, Files.readAllBytes(path), fileName);
            } catch (QqException e) {
                throw e;
            } catch (IOException | RuntimeException e) {
                throw new QqException("Cannot read QQ media file", e);
            }
        }
        throw new QqException("Unsupported QQ media url: " + url);
    }

    private String uploadUrl(int fileType, String url, String fileName) {
        QqModels.UploadRequest request = new QqModels.UploadRequest(
                fileType, url, null, fileName, false);
        return upload(request);
    }

    private String uploadBytes(int fileType, byte[] bytes, String fileName) {
        if (bytes.length > MAX_LOCAL_MEDIA_BYTES)
            throw new QqException("QQ local media exceeds 5 MB");
        QqModels.UploadRequest request = new QqModels.UploadRequest(
                fileType, null, Base64.getEncoder().encodeToString(bytes), fileName, false);
        return upload(request);
    }

    private String upload(QqModels.UploadRequest request) {
        QqModels.UploadResponse response = reply.target().group()
                ? api.uploadGroup(reply.target().id(), request)
                : api.uploadUser(reply.target().id(), request);
        return response.fileInfo();
    }

    // ════ Info ═══════════════════

    @Override
    public UserInfo getLoginInfo() {
        try {
            QqModels.BotInfo info = api.botInfo();
            if (info != null) {
                String id = info.id() == null || info.id().isBlank() ? selfId : info.id();
                String name = info.username() == null || info.username().isBlank() ? id : info.username();
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
        return new UserInfo(userId, name == null || name.isBlank() ? userId : name, null);
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
                    .filter(member -> member != null && member.memberOpenid() != null)
                    .filter(member -> !Boolean.TRUE.equals(member.bot()))
                    .map(member -> new UserInfo(
                            member.memberOpenid(),
                            member.username() == null || member.username().isBlank()
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
        QqMessageIndex.Target target = entry == null
                ? (reply == null ? null : reply.target())
                : entry.target();
        if (target == null) throw new QqException("Cannot resolve QQ message target");
        api.recall(target, entry == null ? messageId : entry.actualId());
    }

    @Override
    public void mute(String groupId, String userId, int seconds) {
        api.mute(groupId, userId, seconds);
    }

    // ════ Helpers ═════════════════

    private void checkContext() {
        if (reply == null) throw activeUnsupported();
        int window = reply.target().group() ? GROUP_PASSIVE_SECONDS : C2C_PASSIVE_SECONDS;
        int max = reply.target().group() ? GROUP_PASSIVE_REPLIES : C2C_PASSIVE_REPLIES;
        if (Instant.now().isAfter(reply.receivedAt().plusSeconds(window)))
            throw new QqException("QQ passive reply window expired");
        if (sent >= max) throw new QqException("QQ passive reply limit reached");
    }

    private int nextSeq() {
        checkContext();
        sent++;
        return seq++;
    }

    private static QqException activeUnsupported() {
        return new QqException("Active messages are not supported by the QQ official bot API");
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

    private static int fileType(MsgSegment.Media media) {
        if (media instanceof MsgSegment.Image) return 1;
        if (media instanceof MsgSegment.Video) return 2;
        return 3;
    }

    private static String fileName(MsgSegment.Media media, String defaultName) {
        String name = media.file();
        if (name == null || name.isBlank()) name = defaultName;
        int slash = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\'));
        if (slash >= 0) name = name.substring(slash + 1);
        if (name.isBlank()) name = defaultName;
        return fileName(name, defaultName);
    }

    private static String fileName(String name, String defaultName) {
        String value = name == null || name.isBlank() ? defaultName : name;
        int query = value.indexOf('?');
        if (query >= 0) value = value.substring(0, query);
        int fragment = value.indexOf('#');
        if (fragment >= 0) value = value.substring(0, fragment);
        int slash = Math.max(value.lastIndexOf('/'), value.lastIndexOf('\\'));
        if (slash >= 0) value = value.substring(slash + 1);
        if (value.isBlank()) value = defaultName;
        if (!value.contains(".")) {
            int dot = defaultName.lastIndexOf('.');
            value += dot >= 0 ? defaultName.substring(dot) : "";
        }
        return value;
    }
}
