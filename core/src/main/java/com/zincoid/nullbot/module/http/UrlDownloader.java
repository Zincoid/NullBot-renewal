package com.zincoid.nullbot.module.http;

import com.zincoid.nullbot.platform.exception.PlatformException;
import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * URL 下载器：按固定超时打开媒体流，供文件与附件服务共用。
 * <p>URL downloader: opens media streams with fixed timeouts, shared by the file and attachment services.</p>
 */
@Component
public class UrlDownloader {

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    /**
     * 打开 URL 的响应流，调用方负责关闭。
     * <p>Opens the response stream of a URL; the caller owns closing it.</p>
     *
     * @param url source URL
     * @return the response body stream
     */
    public InputStream open(String url) {
        if (url == null || url.isBlank())
            throw new PlatformException("Missing key: url");
        HttpRequest request;
        try {
            request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(30))
                    .GET()
                    .build();
        } catch (IllegalArgumentException e) {
            throw new PlatformException("Malformed URL", e);
        }
        try {
            HttpResponse<InputStream> resp = httpClient.send(
                    request, HttpResponse.BodyHandlers.ofInputStream());
            if (resp.statusCode() / 100 != 2) {
                IOUtils.closeQuietly(resp.body());
                throw new PlatformException("Download failed: status=" + resp.statusCode());
            }
            return resp.body();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new PlatformException("Download interrupted", e);
        } catch (IOException e) {
            throw new PlatformException("Download failed", e);
        }
    }
}
