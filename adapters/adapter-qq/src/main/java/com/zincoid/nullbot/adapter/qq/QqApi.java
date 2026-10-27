package com.zincoid.nullbot.adapter.qq;

import com.zincoid.nullbot.adapter.qq.model.QqModels;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * QQ 官方 OpenAPI 领域客户端。
 * <p>QQ official OpenAPI domain client.</p>
 * <p>负责 gateway、消息发送、富媒体上传、撤回、禁言、群资料与缓存；HTTP/token 由 {@link QqHttpClient} 处理。</p>
 * <p>Handles gateways, messages, media uploads, recalls, mutes, group lookups, and caches; HTTP and tokens are delegated to {@link QqHttpClient}.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class QqApi {

    private static final int GROUP_NAME_TTL_SECONDS = 600;
    private static final int GROUP_MEMBERS_TTL_SECONDS = 60;
    private static final int MAX_GROUP_CACHE_ENTRIES = 512;
    private static final int MAX_GROUP_PAGES = 20;
    private static final int MAX_MUTE_SECONDS = 30 * 24 * 60 * 60;
    private static final int BOT_INFO_RETRY_SECONDS = 60;

    private final QqHttpClient http;
    private final Map<String, Cached<String>> groupNames = lruCache(MAX_GROUP_CACHE_ENTRIES);
    private final Map<String, Cached<List<QqModels.Member>>> groupMembers =
            lruCache(MAX_GROUP_CACHE_ENTRIES);

    private volatile QqModels.BotInfo botInfo;
    private volatile Instant botInfoRetryAt = Instant.EPOCH;

    // ════ Gateway ════════════════

    /**
     * 获取 WebSocket 网关地址。
     * <p>Fetches the WebSocket gateway URL.</p>
     *
     * @return the gateway URL
     */
    public String gatewayUrl() {
        QqModels.GatewayResponse response = http.get(
                "/gateway/bot", QqModels.GatewayResponse.class);
        if (response == null || QqText.isBlank(response.url()))
            throw new QqException("Missing QQ gateway url");
        return response.url();
    }

    // ════ Bot info ═══════════════

    /**
     * 返回机器人资料，可能为 null。
     * <p>Returns the bot profile, possibly null.</p>
     *
     * @return the bot profile
     */
    public QqModels.BotInfo botInfo() {
        QqModels.BotInfo info = botInfo;
        if (info != null && !QqText.isBlank(info.avatar())) return info;
        if (Instant.now().isBefore(botInfoRetryAt)) return info;
        try {
            QqModels.BotInfo fetched = http.get("/users/@me", QqModels.BotInfo.class);
            if (fetched != null && (!QqText.isBlank(fetched.id())
                    || !QqText.isBlank(fetched.username())
                    || !QqText.isBlank(fetched.avatar()))) {
                botInfo = fetched;
                botInfoRetryAt = QqText.isBlank(fetched.avatar())
                        ? Instant.now().plusSeconds(BOT_INFO_RETRY_SECONDS) : Instant.EPOCH;
                return fetched;
            }
        } catch (QqException e) {
            log.warn("QQ bot info failed: code={}", e.code());
        }
        botInfoRetryAt = Instant.now().plusSeconds(BOT_INFO_RETRY_SECONDS);
        return info;
    }

    /**
     * 缓存机器人资料。
     * <p>Caches the bot profile.</p>
     *
     * @param info the bot profile
     */
    public void setBotInfo(QqModels.BotInfo info) {
        if (info == null) return;
        this.botInfo = info;
        this.botInfoRetryAt = Instant.EPOCH;
    }

    // ════ Group info ═════════════

    /**
     * 返回群名称，失败时回退为群 ID。
     * <p>Returns the group name, falling back to the group id.</p>
     *
     * @param groupId the group openid
     * @return the group name or id
     */
    public String groupName(String groupId) {
        String cached = fresh(groupNames.get(groupId));
        if (cached != null) return cached;
        String name = groupId;
        try {
            QqModels.GroupInfoData info = http.get(
                    "/v2/groups/" + encode(groupId) + "/info", QqModels.GroupInfoData.class);
            if (info != null && !QqText.isBlank(info.groupName())) name = info.groupName();
        } catch (QqException e) {
            if (e.code() != 11253)
                log.warn("QQ group info failed: group={}, code={}", groupId, e.code());
        }
        groupNames.put(groupId, new Cached<>(name,
                Instant.now().plusSeconds(GROUP_NAME_TTL_SECONDS)));
        return name;
    }

    /**
     * 返回群成员，失败或无权限时返回空列表。
     * <p>Returns group members, or an empty list on failure or missing permission.</p>
     *
     * @param groupId the group openid
     * @return the members
     */
    public List<QqModels.Member> groupMembers(String groupId) {
        List<QqModels.Member> cached = fresh(groupMembers.get(groupId));
        if (cached != null) return cached;
        List<QqModels.Member> members = new ArrayList<>();
        try {
            String cursor = null;
            for (int page = 0; page < MAX_GROUP_PAGES; page++) {
                String path = "/v2/groups/" + encode(groupId) + "/members";
                if (!QqText.isBlank(cursor)) path += "?cursor=" + encode(cursor);
                QqModels.MemberListData data = http.get(path, QqModels.MemberListData.class);
                if (data == null || data.members() == null || data.members().isEmpty()) break;
                for (QqModels.Member member : data.members()) {
                    if (member != null) members.add(member);
                }
                String next = data.nextCursor();
                if (QqText.isBlank(next) || next.equals(cursor)) break;
                cursor = next;
            }
        } catch (QqException e) {
            if (e.code() != 11253)
                log.warn("QQ group members failed: group={}, code={}", groupId, e.code());
        }
        List<QqModels.Member> result = List.copyOf(members);
        groupMembers.put(groupId, new Cached<>(result,
                Instant.now().plusSeconds(GROUP_MEMBERS_TTL_SECONDS)));
        return result;
    }

    // ════ Messages ═══════════════

    /**
     * 发送文本消息。
     * <p>Sends a text message.</p>
     *
     * @param target the target conversation
     * @param text   the text
     * @return the platform message id
     */
    public String sendText(QqTarget target, String text) {
        return sendMessage(targetPath(target) + "/messages",
                new QqModels.SendRequest(0, text));
    }

    private String sendMessage(String path, QqModels.SendRequest request) {
        QqModels.SendResponse response = http.post(
                path, request, QqModels.SendResponse.class);
        if (response == null || QqText.isBlank(response.id()))
            throw new QqException("Missing QQ message id in send response");
        return response.id();
    }

    // ════ Upload ═════════════════

    /**
     * 上传并直接发送富媒体。
     * <p>Uploads rich media and sends it directly.</p>
     *
     * @param target  the target conversation
     * @param request the upload request
     * @return the upload response, containing the sent message id
     */
    public QqModels.UploadResponse upload(QqTarget target, QqModels.UploadRequest request) {
        QqModels.UploadResponse response = http.post(
                targetPath(target) + "/files", request, QqModels.UploadResponse.class);
        if (response == null || QqText.isBlank(response.id()))
            throw new QqException("Missing QQ upload message id");
        return response;
    }

    // ════ Management ═════════════

    /**
     * 撤回消息。
     * <p>Recalls a message.</p>
     *
     * @param target    the target conversation
     * @param messageId the platform message id
     */
    public void recall(QqTarget target, String messageId) {
        http.delete(targetPath(target) + "/messages/" + encode(messageId));
    }

    /**
     * 设置群成员禁言，0 秒表示解除。
     * <p>Mutes a group member; zero seconds unmutes.</p>
     *
     * @param groupId      the group openid
     * @param memberOpenid the member openid
     * @param seconds      the mute duration in seconds
     */
    public void mute(String groupId, String memberOpenid, int seconds) {
        String op = seconds <= 0 ? "del" : "add";
        String expire = seconds <= 0 ? null : OffsetDateTime.now()
                .plusSeconds(Math.min(seconds, MAX_MUTE_SECONDS))
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        http.post("/v2/groups/" + encode(groupId) + "/restrict_chat_setting",
                new QqModels.MuteRequest(List.of(
                        new QqModels.MuteMember(op, memberOpenid, expire))), null);
    }

    // ════ Helpers ═════════════════

    private static String targetPath(QqTarget target) {
        if (target == null || QqText.isBlank(target.id()))
            throw new QqException("Missing QQ target");
        return (target.group() ? "/v2/groups/" : "/v2/users/") + encode(target.id());
    }

    private static <T> T fresh(Cached<T> cached) {
        return cached != null && Instant.now().isBefore(cached.expiresAt())
                ? cached.value() : null;
    }

    private static <K, V> Map<K, V> lruCache(int maxEntries) {
        return Collections.synchronizedMap(new LinkedHashMap<>(64, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                return size() > maxEntries;
            }
        });
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private record Cached<T>(T value, Instant expiresAt) {
    }
}
