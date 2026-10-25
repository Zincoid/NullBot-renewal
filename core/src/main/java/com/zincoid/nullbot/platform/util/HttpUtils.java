package com.zincoid.nullbot.platform.util;

import com.zincoid.nullbot.platform.exception.PlatformException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * HTTP 读取工具：限制协议、超时与响应体大小。
 * <p>HTTP read helper: restricts protocol, timeout, and response body size.</p>
 */
public final class HttpUtils {

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private HttpUtils() {
    }

    /**
     * 下载指定 URL 的字节内容。
     * <p>Downloads the bytes of the given URL.</p>
     *
     * @param url      the http or https url
     * @param maxBytes the maximum accepted body size
     * @return the downloaded bytes
     */
    public static byte[] getBytes(String url, int maxBytes) {
        if (url == null || url.isBlank()) throw new PlatformException("Missing key: url");
        if (maxBytes <= 0) throw new PlatformException("Invalid max bytes: " + maxBytes);
        URI uri;
        try {
            uri = URI.create(url);
        } catch (IllegalArgumentException e) {
            throw new PlatformException("Malformed url", e);
        }
        String scheme = uri.getScheme();
        if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme))
            throw new PlatformException("Unsupported url scheme: " + scheme);
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(20))
                .GET()
                .build();
        try {
            HttpResponse<InputStream> response = CLIENT.send(
                    request, HttpResponse.BodyHandlers.ofInputStream());
            try (InputStream in = response.body()) {
                if (response.statusCode() / 100 != 2)
                    throw new PlatformException("Download failed: status=" + response.statusCode());
                long length = response.headers().firstValueAsLong("Content-Length").orElse(-1);
                if (length > maxBytes)
                    throw new PlatformException("Download too large: " + length);
                byte[] bytes = in.readNBytes(maxBytes + 1);
                if (bytes.length > maxBytes)
                    throw new PlatformException("Download too large");
                return bytes;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new PlatformException("Download interrupted", e);
        } catch (IOException e) {
            throw new PlatformException("Download failed", e);
        }
    }
}
