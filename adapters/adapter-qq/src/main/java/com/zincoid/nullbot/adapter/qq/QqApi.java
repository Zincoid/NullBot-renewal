package com.zincoid.nullbot.adapter.qq;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zincoid.nullbot.adapter.qq.model.QqModels;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * QQ 官方 OpenAPI 客户端。
 * <p>QQ official OpenAPI client.</p>
 * <p>负责 token、HTTP 请求、gateway、消息发送、富媒体上传与群资料查询。</p>
 * <p>Handles tokens, HTTP requests, gateways, message sending, media uploads, and group lookups.</p>
 */
@Slf4j
@Component
public class QqApi {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);
    private static final int TOKEN_MARGIN_SECONDS = 60;
    private static final int GROUP_NAME_TTL_SECONDS = 600;
    private static final int GROUP_MEMBERS_TTL_SECONDS = 60;
    private static final int MAX_GROUP_PAGES = 20;
    private static final int MAX_MUTE_SECONDS = 30 * 24 * 60 * 60;

    private final QqProperties properties;
    private final ObjectMapper mapper;
    private final HttpClient http;
    private final Map<String, Cached<String>> groupNames = new ConcurrentHashMap<>();
    private final Map<String, Cached<List<QqModels.Member>>> groupMembers = new ConcurrentHashMap<>();

    private volatile Token token;
    private volatile QqModels.BotInfo botInfo;

    /**
     * 创建 API 客户端。
     * <p>Creates the API client.</p>
     *
     * @param properties the adapter properties
     */
    public QqApi(QqProperties properties) {
        this.properties = properties;
        this.mapper = new ObjectMapper()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .setSerializationInclusion(JsonInclude.Include.NON_NULL);
        this.http = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    // ════ JSON ═══════════════════

    /**
     * 解析 WebSocket 下行数据包。
     * <p>Parses a WebSocket downstream payload.</p>
     *
     * @param json the JSON text
     * @return the payload
     */
    public QqModels.Payload parsePayload(String json) {
        return read(json, QqModels.Payload.class);
    }

    /**
     * 把对象序列化为 JSON。
     * <p>Serializes an object to JSON.</p>
     *
     * @param value the object
     * @return the JSON text
     */
    public String json(Object value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (IOException e) {
            throw new QqException("Cannot encode QQ request", e);
        }
    }

    /**
     * 把 JSON 树读取为指定类型。
     * <p>Reads a JSON tree as the given type.</p>
     *
     * @param node the JSON tree
     * @param type the target type
     * @param <T>  the target type
     * @return the value, or null when the tree is null
     */
    public <T> T read(JsonNode node, Class<T> type) {
        if (node == null || node.isNull()) return null;
        try {
            return mapper.treeToValue(node, type);
        } catch (IOException e) {
            throw new QqException("Cannot decode QQ payload", e);
        }
    }

    // ════ Token ══════════════════

    /**
     * 返回有效的 access token，必要时刷新。
     * <p>Returns a valid access token, refreshing it when needed.</p>
     *
     * @return the access token
     */
    public String token() {
        Token current = token;
        if (current != null && Instant.now().isBefore(current.expiresAt())) return current.value();
        synchronized (this) {
            current = token;
            if (current != null && Instant.now().isBefore(current.expiresAt())) return current.value();
            Token refreshed = refreshToken();
            token = refreshed;
            log.info("QQ access token refreshed");
            return refreshed.value();
        }
    }

    /**
     * 丢弃当前 access token。
     * <p>Discards the current access token.</p>
     */
    public void invalidateToken() {
        token = null;
    }

    private Token refreshToken() {
        String body = rawPost(properties.resolvedTokenUrl(),
                new QqModels.TokenRequest(properties.getAppId(), properties.getAppSecret()));
        if (body == null || body.isBlank())
            throw new QqException("Empty QQ token response");
        try {
            JsonNode node = mapper.readTree(body);
            if (node == null) throw new QqException("Empty QQ token response");
            String access = node.path("access_token").asText(null);
            if (access == null || access.isBlank()) {
                int code = errorCode(node);
                String message = node.path("message").asText("QQ token request failed");
                throw new QqException(200, code, message);
            }
            long expires = seconds(node.path("expires_in"));
            return new Token(access, Instant.now().plusSeconds(Math.max(30, expires - TOKEN_MARGIN_SECONDS)));
        } catch (IOException e) {
            throw new QqException("Cannot decode QQ token response", e);
        }
    }

    // ════ Gateway ════════════════

    /**
     * 获取 WebSocket 网关地址。
     * <p>Fetches the WebSocket gateway URL.</p>
     *
     * @return the gateway URL
     */
    public String gatewayUrl() {
        QqModels.GatewayResponse response = get("/gateway/bot", QqModels.GatewayResponse.class);
        if (response == null || response.url() == null || response.url().isBlank())
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
        if (info != null && info.avatar() != null && !info.avatar().isBlank()) return info;
        try {
            QqModels.BotInfo fetched = get("/users/@me", QqModels.BotInfo.class);
            if (fetched != null) {
                botInfo = fetched;
                return fetched;
            }
        } catch (QqException e) {
            log.warn("QQ bot info failed: code={}", e.code());
        }
        return info;
    }

    /**
     * 缓存机器人资料。
     * <p>Caches the bot profile.</p>
     *
     * @param info the bot profile
     */
    public void setBotInfo(QqModels.BotInfo info) {
        this.botInfo = info;
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
        Cached<String> cached = groupNames.get(groupId);
        if (cached != null && Instant.now().isBefore(cached.expiresAt())) return cached.value();
        String name = groupId;
        try {
            QqModels.GroupInfoData info = get(
                    "/v2/groups/" + encode(groupId) + "/info", QqModels.GroupInfoData.class);
            if (info != null && info.groupName() != null && !info.groupName().isBlank())
                name = info.groupName();
        } catch (QqException e) {
            if (e.code() != 11253)
                log.warn("QQ group info failed: group={}, code={}", groupId, e.code());
        }
        groupNames.put(groupId, new Cached<>(name, Instant.now().plusSeconds(GROUP_NAME_TTL_SECONDS)));
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
        Cached<List<QqModels.Member>> cached = groupMembers.get(groupId);
        if (cached != null && Instant.now().isBefore(cached.expiresAt())) return cached.value();
        List<QqModels.Member> members = new ArrayList<>();
        try {
            String cursor = null;
            for (int page = 0; page < MAX_GROUP_PAGES; page++) {
                String path = "/v2/groups/" + encode(groupId) + "/members";
                if (cursor != null && !cursor.isBlank()) path += "?cursor=" + encode(cursor);
                QqModels.MemberListData data = get(path, QqModels.MemberListData.class);
                if (data == null || data.members() == null || data.members().isEmpty()) break;
                members.addAll(data.members());
                String next = data.nextCursor();
                if (next == null || next.isBlank() || next.equals(cursor)) break;
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
     * 发送群文本消息。
     * <p>Sends a group text message.</p>
     *
     * @param groupId the group openid
     * @param text    the text
     * @param msgId   the passive reply message id
     * @param seq     the passive reply sequence
     * @return the platform message id
     */
    public String sendGroupText(String groupId, String text, String msgId, int seq) {
        return sendMessage("/v2/groups/" + encode(groupId) + "/messages",
                new QqModels.SendRequest(0, text, msgId, seq, null));
    }

    /**
     * 发送群富媒体消息。
     * <p>Sends a group rich-media message.</p>
     *
     * @param groupId  the group openid
     * @param fileInfo the uploaded file info
     * @param msgId    the passive reply message id
     * @param seq      the passive reply sequence
     * @return the platform message id
     */
    public String sendGroupMedia(String groupId, String fileInfo, String msgId, int seq) {
        return sendMessage("/v2/groups/" + encode(groupId) + "/messages",
                new QqModels.SendRequest(7, " ", msgId, seq, new QqModels.Media(fileInfo)));
    }

    /**
     * 发送 C2C 文本消息。
     * <p>Sends a C2C text message.</p>
     *
     * @param userId the user openid
     * @param text   the text
     * @param msgId  the passive reply message id
     * @param seq    the passive reply sequence
     * @return the platform message id
     */
    public String sendUserText(String userId, String text, String msgId, int seq) {
        return sendMessage("/v2/users/" + encode(userId) + "/messages",
                new QqModels.SendRequest(0, text, msgId, seq, null));
    }

    /**
     * 发送 C2C 富媒体消息。
     * <p>Sends a C2C rich-media message.</p>
     *
     * @param userId   the user openid
     * @param fileInfo the uploaded file info
     * @param msgId    the passive reply message id
     * @param seq      the passive reply sequence
     * @return the platform message id
     */
    public String sendUserMedia(String userId, String fileInfo, String msgId, int seq) {
        return sendMessage("/v2/users/" + encode(userId) + "/messages",
                new QqModels.SendRequest(7, " ", msgId, seq, new QqModels.Media(fileInfo)));
    }

    private String sendMessage(String path, QqModels.SendRequest request) {
        QqModels.SendResponse response = post(path, request, QqModels.SendResponse.class);
        if (response == null || response.id() == null || response.id().isBlank())
            throw new QqException("Missing QQ message id in send response");
        return response.id();
    }

    // ════ Upload ═════════════════

    /**
     * 上传群富媒体。
     * <p>Uploads group rich media.</p>
     *
     * @param groupId the group openid
     * @param request the upload request
     * @return the upload response
     */
    public QqModels.UploadResponse uploadGroup(String groupId, QqModels.UploadRequest request) {
        return upload("/v2/groups/" + encode(groupId) + "/files", request);
    }

    /**
     * 上传 C2C 富媒体。
     * <p>Uploads C2C rich media.</p>
     *
     * @param userId  the user openid
     * @param request the upload request
     * @return the upload response
     */
    public QqModels.UploadResponse uploadUser(String userId, QqModels.UploadRequest request) {
        return upload("/v2/users/" + encode(userId) + "/files", request);
    }

    private QqModels.UploadResponse upload(String path, QqModels.UploadRequest request) {
        QqModels.UploadResponse response = post(path, request, QqModels.UploadResponse.class);
        if (response == null || response.fileInfo() == null || response.fileInfo().isBlank())
            throw new QqException("Missing QQ file_info in upload response");
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
    public void recall(QqMessageIndex.Target target, String messageId) {
        String path = target.group()
                ? "/v2/groups/" + encode(target.id()) + "/messages/" + encode(messageId)
                : "/v2/users/" + encode(target.id()) + "/messages/" + encode(messageId);
        delete(path);
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
        String expire = seconds <= 0 ? "" : OffsetDateTime.now()
                .plusSeconds(Math.min(seconds, MAX_MUTE_SECONDS))
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        post("/v2/groups/" + encode(groupId) + "/restrict_chat_setting",
                new QqModels.MuteRequest(List.of(
                        new QqModels.MuteMember(op, memberOpenid, expire))), null);
    }

    // ════ HTTP ═══════════════════

    private <T> T get(String path, Class<T> type) {
        return parse(send("GET", path, null), type);
    }

    private <T> T post(String path, Object body, Class<T> type) {
        return parse(send("POST", path, body), type);
    }

    private void delete(String path) {
        send("DELETE", path, null);
    }

    private String send(String method, String path, Object body) {
        try {
            return sendOnce(method, path, body);
        } catch (QqException e) {
            if (isTokenError(e)) {
                invalidateToken();
                return sendOnce(method, path, body);
            }
            throw e;
        }
    }

    private String sendOnce(String method, String path, Object body) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri(path))
                .timeout(REQUEST_TIMEOUT)
                .header("Authorization", "QQBot " + token());
        if (body == null) {
            builder.method(method, HttpRequest.BodyPublishers.noBody());
        } else {
            builder.header("Content-Type", "application/json; charset=utf-8")
                    .method(method, HttpRequest.BodyPublishers.ofString(json(body), StandardCharsets.UTF_8));
        }
        try {
            HttpResponse<String> response = http.send(builder.build(),
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            int status = response.statusCode();
            String text = response.body();
            if (status < 200 || status >= 300) throw apiError(status, text);
            if (text != null && !text.isBlank()) {
                JsonNode node = mapper.readTree(text);
                int code = errorCode(node);
                if (code != 0) throw new QqException(status, code, errorMessage(node));
            }
            return text;
        } catch (IOException e) {
            throw new QqException("QQ request failed: " + method + " " + path, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new QqException("QQ request interrupted: " + method + " " + path, e);
        }
    }

    private String rawPost(String url, Object body) {
        HttpRequest request = HttpRequest.newBuilder(uri(url))
                .timeout(REQUEST_TIMEOUT)
                .header("Content-Type", "application/json; charset=utf-8")
                .POST(HttpRequest.BodyPublishers.ofString(json(body), StandardCharsets.UTF_8))
                .build();
        try {
            HttpResponse<String> response = http.send(request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            int status = response.statusCode();
            String text = response.body();
            if (status < 200 || status >= 300) throw apiError(status, text);
            return text;
        } catch (IOException e) {
            throw new QqException("QQ token request failed", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new QqException("QQ token request interrupted", e);
        }
    }

    /**
     * 下载远程资源，超过上限时失败。
     * <p>Downloads a remote resource and fails when the limit is exceeded.</p>
     *
     * @param url      the resource URL
     * @param maxBytes the maximum accepted size
     * @return the bytes
     */
    public byte[] download(String url, int maxBytes) {
        URI uri;
        try {
            uri = URI.create(url);
        } catch (IllegalArgumentException e) {
            throw new QqException("Malformed QQ media url: " + url, e);
        }
        if (!"http".equalsIgnoreCase(uri.getScheme()) && !"https".equalsIgnoreCase(uri.getScheme()))
            throw new QqException("Unsupported QQ media url: " + url);
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(REQUEST_TIMEOUT)
                .GET()
                .build();
        try {
            HttpResponse<InputStream> response = http.send(request,
                    HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() < 200 || response.statusCode() >= 300)
                throw new QqException(response.statusCode(), 0, "QQ media download failed");
            try (InputStream input = response.body()) {
                byte[] bytes = input.readNBytes(maxBytes + 1);
                if (bytes.length > maxBytes)
                    throw new QqException("QQ media exceeds " + maxBytes + " bytes");
                return bytes;
            }
        } catch (IOException e) {
            throw new QqException("QQ media download failed", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new QqException("QQ media download interrupted", e);
        }
    }

    // ════ Helpers ═════════════════

    private <T> T parse(String text, Class<T> type) {
        if (type == null || text == null || text.isBlank()) return null;
        try {
            return mapper.readValue(text, type);
        } catch (IOException e) {
            throw new QqException("Cannot decode QQ response", e);
        }
    }

    private <T> T read(String json, Class<T> type) {
        try {
            return mapper.readValue(json, type);
        } catch (IOException e) {
            throw new QqException("Cannot decode QQ payload", e);
        }
    }

    private QqException apiError(int status, String body) {
        if (body == null || body.isBlank()) return new QqException(status, 0, "QQ API error: " + status);
        try {
            JsonNode node = mapper.readTree(body);
            return new QqException(status, errorCode(node), errorMessage(node));
        } catch (IOException e) {
            return new QqException(status, 0, "QQ API error: " + status);
        }
    }

    private static int errorCode(JsonNode node) {
        if (node == null) return 0;
        int code = node.path("err_code").asInt(0);
        return code != 0 ? code : node.path("code").asInt(0);
    }

    private static String errorMessage(JsonNode node) {
        if (node == null) return "QQ API error";
        String message = node.path("message").asText("");
        return message.isBlank() ? "QQ API error" : message;
    }

    private static boolean isTokenError(QqException e) {
        if (e.status() == 401) return true;
        int code = e.code();
        return code == 11243 || code == 11244 || code == 11245;
    }

    private URI uri(String path) {
        if (path.startsWith("http://") || path.startsWith("https://")) return URI.create(path);
        String base = properties.resolvedApiBase();
        return URI.create(base + (path.startsWith("/") ? path : "/" + path));
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private static long seconds(JsonNode node) {
        if (node == null || node.isNull()) return 7200;
        if (node.isTextual()) {
            try {
                return Long.parseLong(node.asText());
            } catch (NumberFormatException e) {
                return 7200;
            }
        }
        long value = node.asLong();
        return value <= 0 ? 7200 : value;
    }

    private record Token(String value, Instant expiresAt) {
    }

    private record Cached<T>(T value, Instant expiresAt) {
    }
}
