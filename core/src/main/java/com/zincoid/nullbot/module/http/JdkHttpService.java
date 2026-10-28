package com.zincoid.nullbot.module.http;

import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 基于 JDK HttpClient 的宿主 HTTP 服务。
 * <p>Host HTTP service backed by the JDK HttpClient.</p>
 */
@Component
public final class JdkHttpService implements HttpService {

    private static final Duration RETRY_DELAY = Duration.ofMillis(250);
    private static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";

    private final Map<ClientKey, HttpClient> clients = new ConcurrentHashMap<>();

    @Override
    public Response get(String url, Options options) {
        return send("GET", url, null, null, options);
    }

    @Override
    public Response post(String url, byte[] body, String contentType, Options options) {
        return send("POST", url, body, contentType, options);
    }

    @Override
    public Response upload(String url, String fieldName, String filename,
                           byte[] content, Options options) {
        if (fieldName == null || fieldName.isBlank())
            throw new HttpException("Blank multipart field name");
        Multipart multipart = multipart(fieldName, filename, content);
        return send("POST", url, multipart.body(), multipart.contentType(), options);
    }

    // ════ Request ═════════════════

    private Response send(String method, String url, byte[] body, String contentType,
                          Options options) {
        Options opts = options == null ? Options.defaults() : options;
        URI uri = uri(url);
        HttpClient client = client(opts);
        HttpRequest request = request(method, uri, body, contentType, opts);
        IOException last = null;
        for (int attempt = 1; attempt <= opts.retries() + 1; attempt++) {
            try {
                return execute(client, request, opts.maxBytes());
            } catch (IOException e) {
                last = e;
                if (attempt <= opts.retries()) sleep();
            }
        }
        throw new HttpException("HTTP request failed: " + method, last);
    }

    private static HttpRequest request(String method, URI uri, byte[] body,
                                       String contentType, Options options) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder(uri)
                    .timeout(options.requestTimeout());
            for (Map.Entry<String, String> header : options.headers().entrySet()) {
                try {
                    builder.header(header.getKey(), header.getValue());
                } catch (IllegalArgumentException e) {
                    throw new HttpException("Invalid header: " + header.getKey(), e);
                }
            }
            if ("GET".equals(method)) {
                builder.GET();
            } else {
                byte[] payload = body == null ? new byte[0] : body;
                String type = contentType == null || contentType.isBlank()
                        ? DEFAULT_CONTENT_TYPE : contentType;
                builder.setHeader("Content-Type", type);
                builder.POST(HttpRequest.BodyPublishers.ofByteArray(payload));
            }
            return builder.build();
        } catch (HttpException e) {
            throw e;
        } catch (IllegalArgumentException e) {
            throw new HttpException("Invalid HTTP request", e);
        }
    }

    private static Response execute(HttpClient client, HttpRequest request, int maxBytes)
            throws IOException {
        HttpResponse<InputStream> response;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new HttpException("HTTP request interrupted", e);
        }
        try (InputStream in = response.body()) {
            long length = response.headers().firstValueAsLong("Content-Length").orElse(-1);
            if (length > maxBytes)
                throw new HttpException("Response too large: " + length);
            byte[] body = in.readNBytes(maxBytes + 1);
            if (body.length > maxBytes)
                throw new HttpException("Response too large");
            String contentType = response.headers()
                    .firstValue("Content-Type")
                    .orElse("");
            return new Response(response.statusCode(), body, contentType);
        }
    }

    private static URI uri(String url) {
        if (url == null || url.isBlank())
            throw new HttpException("Missing URL");
        try {
            URI uri = URI.create(url);
            String scheme = uri.getScheme();
            if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme))
                throw new HttpException("Unsupported URL scheme: " + scheme);
            return uri;
        } catch (IllegalArgumentException e) {
            throw new HttpException("Malformed URL", e);
        }
    }

    private static void sleep() {
        try {
            Thread.sleep(RETRY_DELAY.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new HttpException("HTTP request interrupted", e);
        }
    }

    // ════ Client ══════════════════

    private HttpClient client(Options options) {
        ClientKey key = new ClientKey(options.proxy(), options.connectTimeout());
        return clients.computeIfAbsent(key, JdkHttpService::buildClient);
    }

    private static HttpClient buildClient(ClientKey key) {
        HttpClient.Builder builder = HttpClient.newBuilder()
                .connectTimeout(key.connectTimeout())
                .followRedirects(HttpClient.Redirect.NORMAL)
                .version(HttpClient.Version.HTTP_1_1);
        if (!key.proxy().isBlank()) builder.proxy(proxy(key.proxy()));
        return builder.build();
    }

    private static ProxySelector proxy(String raw) {
        ProxyAddress address;
        try {
            address = ProxyAddress.parse(raw);
        } catch (IllegalArgumentException e) {
            throw new HttpException("Invalid proxy: " + raw, e);
        }
        return ProxySelector.of(InetSocketAddress.createUnresolved(
                address.host(), address.port()));
    }

    // ════ Multipart ═══════════════

    private static Multipart multipart(String fieldName, String filename, byte[] content) {
        String boundary = "----NullBot" + UUID.randomUUID().toString().replace("-", "");
        String name = safePart(fieldName, "file");
        String file = safePart(filename, "file");
        byte[] bytes = content == null ? new byte[0] : content;

        ByteArrayOutputStream body = new ByteArrayOutputStream();
        write(body, "--" + boundary + "\r\n");
        write(body, "Content-Disposition: form-data; name=\"" + name
                + "\"; filename=\"" + file + "\"\r\n");
        write(body, "Content-Type: " + DEFAULT_CONTENT_TYPE + "\r\n\r\n");
        body.writeBytes(bytes);
        write(body, "\r\n--" + boundary + "--\r\n");
        return new Multipart(body.toByteArray(),
                "multipart/form-data; boundary=" + boundary);
    }

    private static String safePart(String value, String fallback) {
        if (value == null || value.isBlank()) return fallback;
        String safe = value.replace("\r", "").replace("\n", "").replace("\"", "");
        return safe.isBlank() ? fallback : safe;
    }

    private static void write(ByteArrayOutputStream body, String text) {
        body.writeBytes(text.getBytes(StandardCharsets.UTF_8));
    }

    private record Multipart(byte[] body, String contentType) {
    }

    private record ClientKey(String proxy, Duration connectTimeout) {
    }
}
