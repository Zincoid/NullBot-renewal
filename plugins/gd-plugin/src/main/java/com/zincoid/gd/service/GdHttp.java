package com.zincoid.gd.service;

import com.zincoid.gd.GdException;
import com.zincoid.gd.config.GdConfig;
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

/**
 * GD 插件使用的 HTTP 工具：限制超时、重试与响应体大小。
 * <p>HTTP helper for the GD plugin: limits timeouts, retries, and response body size.</p>
 */
final class GdHttp {

    private static final long RETRY_DELAY_MILLIS = 250;

    private final HttpClient client;
    private final String userAgent;
    private final Duration requestTimeout;
    private final int attempts;

    /**
     * 创建 HTTP 工具。
     * <p>Creates the HTTP helper.</p>
     *
     * @param config the plugin configuration
     */
    GdHttp(GdConfig config) {
        HttpClient.Builder builder = HttpClient.newBuilder()
                .connectTimeout(config.connectTimeout())
                .followRedirects(HttpClient.Redirect.NORMAL)
                .version(HttpClient.Version.HTTP_1_1);
        if (!config.proxy().isBlank()) builder.proxy(proxy(config.proxy()));
        this.client = builder.build();
        this.userAgent = config.userAgent();
        this.requestTimeout = config.requestTimeout();
        this.attempts = config.retries() + 1;
    }

    /**
     * 发送 GET 请求。
     * <p>Sends a GET request.</p>
     *
     * @param url      the request URL
     * @param accept   the accept header, nullable
     * @param maxBytes the maximum accepted body size
     * @return the response
     */
    Response get(String url, String accept, int maxBytes) {
        HttpRequest.Builder builder;
        try {
            builder = HttpRequest.newBuilder(URI.create(url))
                    .timeout(requestTimeout)
                    .header("User-Agent", userAgent)
                    .GET();
        } catch (IllegalArgumentException e) {
            throw new GdException("Malformed URL: " + url, e);
        }
        if (accept != null && !accept.isBlank()) builder.header("Accept", accept);
        HttpRequest request = builder.build();
        IOException last = null;
        for (int attempt = 1; attempt <= attempts; attempt++) {
            try {
                HttpResponse<InputStream> response = client.send(
                        request, HttpResponse.BodyHandlers.ofInputStream());
                try (InputStream in = response.body()) {
                    long length = response.headers().firstValueAsLong("Content-Length").orElse(-1);
                    if (length > maxBytes) throw new GdException("Response too large: " + length);
                    byte[] body = in.readNBytes(maxBytes + 1);
                    if (body.length > maxBytes) throw new GdException("Response too large");
                    String contentType = response.headers()
                            .firstValue("Content-Type")
                            .orElse("");
                    return new Response(response.statusCode(), body, contentType);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new GdException("HTTP request interrupted", e);
            } catch (IOException e) {
                last = e;
                if (attempt < attempts) {
                    try {
                        Thread.sleep(RETRY_DELAY_MILLIS);
                    } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                        throw new GdException("HTTP request interrupted", interrupted);
                    }
                }
            }
        }
        throw new GdException("HTTP request failed", last);
    }

    private static ProxySelector proxy(String raw) {
        String value = raw.trim();
        if (value.startsWith("http://")) value = value.substring("http://".length());
        else if (value.startsWith("https://")) value = value.substring("https://".length());
        int split = value.lastIndexOf(':');
        if (split <= 0 || split == value.length() - 1)
            throw new GdException("Invalid proxy: " + raw);
        String host = value.substring(0, split);
        if (host.startsWith("[") && host.endsWith("]"))
            host = host.substring(1, host.length() - 1);
        int port;
        try {
            port = Integer.parseInt(value.substring(split + 1));
        } catch (NumberFormatException e) {
            throw new GdException("Invalid proxy: " + raw, e);
        }
        if (port < 1 || port > 65535) throw new GdException("Invalid proxy: " + raw);
        return ProxySelector.of(InetSocketAddress.createUnresolved(host, port));
    }

    /**
     * HTTP 响应。
     * <p>An HTTP response.</p>
     *
     * @param status      the status code
     * @param body        the response body
     * @param contentType the content type header
     */
    record Response(int status, byte[] body, String contentType) {

        /**
         * 是否返回 2xx。
         * <p>Whether the status is 2xx.</p>
         *
         * @return true for a 2xx status
         */
        boolean ok() {
            return status / 100 == 2;
        }

        /**
         * 以 UTF-8 解码响应体。
         * <p>Decodes the body as UTF-8.</p>
         *
         * @return the decoded text
         */
        String text() {
            return new String(body, StandardCharsets.UTF_8);
        }
    }
}
