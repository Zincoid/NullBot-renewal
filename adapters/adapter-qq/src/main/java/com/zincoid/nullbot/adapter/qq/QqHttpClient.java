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
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;

/**
 * QQ 官方 HTTP 传输与 token 管理。
 * <p>QQ official HTTP transport and token management.</p>
 * <p>负责 JSON、鉴权、错误解析、token 刷新与媒体下载。</p>
 * <p>Handles JSON, authentication, error parsing, token refresh, and media downloads.</p>
 */
@Slf4j
@Component
public class QqHttpClient {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);
    private static final int TOKEN_MARGIN_SECONDS = 60;

    private final QqProperties properties;
    private final ObjectMapper mapper;
    private final HttpClient http;
    private volatile Token token;

    /**
     * 创建 HTTP 客户端。
     * <p>Creates the HTTP client.</p>
     *
     * @param properties the adapter properties
     */
    public QqHttpClient(QqProperties properties) {
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
     * 把 JSON 文本读取为指定类型。
     * <p>Reads JSON text as the given type.</p>
     *
     * @param json the JSON text
     * @param type the target type
     * @param <T>  the target type
     * @return the value, or null when the text is blank
     */
    public <T> T read(String json, Class<T> type) {
        if (QqText.isBlank(json)) return null;
        try {
            return mapper.readValue(json, type);
        } catch (IOException e) {
            throw new QqException("Cannot decode QQ payload", e);
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
        if (QqText.isBlank(body)) throw new QqException("Empty QQ token response");
        try {
            JsonNode node = mapper.readTree(body);
            if (node == null) throw new QqException("Empty QQ token response");
            String access = node.path("access_token").asText(null);
            if (QqText.isBlank(access)) {
                int code = errorCode(node);
                String message = node.path("message").asText("QQ token request failed");
                throw new QqException(200, code, message);
            }
            long expires = seconds(node.path("expires_in"));
            return new Token(access,
                    Instant.now().plusSeconds(Math.max(30, expires - TOKEN_MARGIN_SECONDS)));
        } catch (IOException e) {
            throw new QqException("Cannot decode QQ token response", e);
        }
    }

    // ════ Requests ═══════════════

    /**
     * 发送 GET 请求并解析响应。
     * <p>Sends a GET request and parses the response.</p>
     *
     * @param path the API path
     * @param type the response type, or null to ignore the body
     * @param <T>  the response type
     * @return the parsed response, or null
     */
    public <T> T get(String path, Class<T> type) {
        return parse(send("GET", path, null), type);
    }

    /**
     * 发送 POST 请求并解析响应。
     * <p>Sends a POST request and parses the response.</p>
     *
     * @param path the API path
     * @param body the request body
     * @param type the response type, or null to ignore the body
     * @param <T>  the response type
     * @return the parsed response, or null
     */
    public <T> T post(String path, Object body, Class<T> type) {
        return parse(send("POST", path, body), type);
    }

    /**
     * 发送 DELETE 请求。
     * <p>Sends a DELETE request.</p>
     *
     * @param path the API path
     */
    public void delete(String path) {
        send("DELETE", path, null);
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

    // ════ HTTP ═══════════════════

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
                    .method(method, HttpRequest.BodyPublishers.ofString(
                            json(body), StandardCharsets.UTF_8));
        }
        try {
            HttpResponse<String> response = http.send(builder.build(),
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            int status = response.statusCode();
            String text = response.body();
            if (status < 200 || status >= 300) throw apiError(status, text);
            if (!QqText.isBlank(text)) {
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

    // ════ Helpers ═════════════════

    private <T> T parse(String text, Class<T> type) {
        if (type == null || QqText.isBlank(text)) return null;
        return read(text, type);
    }

    private QqException apiError(int status, String body) {
        if (QqText.isBlank(body))
            return new QqException(status, 0, "QQ API error: " + status);
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
        return QqText.isBlank(message) ? "QQ API error" : message;
    }

    private static boolean isTokenError(QqException e) {
        if (e.status() == 401) return true;
        int code = e.code();
        return code == 11243 || code == 11244 || code == 11245;
    }

    private URI uri(String path) {
        if (path.startsWith("http://") || path.startsWith("https://")) return URI.create(path);
        String base = properties.resolvedApiUrl();
        return URI.create(base + (path.startsWith("/") ? path : "/" + path));
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
}
