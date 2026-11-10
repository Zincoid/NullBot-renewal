package com.zincoid.nullbot.module.http;

import com.zincoid.nullbot.platform.util.AtomicFiles;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;

/**
 * URL 下载工具：按固定超时打开媒体流或写入磁盘，供文件、附件服务与插件共用。
 * <p>URL download utility: opens media streams or writes them to disk with fixed timeouts,
 * shared by the file and attachment services and plugins.</p>
 */
public final class UrlDownloader {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);

    private static final HttpClient HTTP_CLIENT =
            HttpSupport.client("", CONNECT_TIMEOUT, false);

    private UrlDownloader() {
    }

    /**
     * 打开 URL 的响应流，调用方负责关闭。
     * <p>Opens the response stream of a URL; the caller owns closing it.</p>
     *
     * @param url source URL
     * @return the response body stream
     */
    public static InputStream open(String url) {
        return stream(url);
    }

    /**
     * 下载 URL 到目标文件，先写同目录临时文件，成功后原子替换目标。
     * <p>Downloads a URL into a target file, writing a sibling temporary file first
     * and atomically replacing the target on success.</p>
     * <p>目标已存在时覆盖，父目录不存在时自动创建；失败时清理临时文件并保留原目标。</p>
     * <p>An existing target is overwritten and missing parent directories are created;
     * on failure the temporary file is removed and the previous target is kept.</p>
     *
     * @param url    source URL
     * @param target target file
     * @return the target file
     */
    public static Path download(String url, Path target) {
        if (target == null) throw new HttpException("Missing target");
        try (InputStream in = stream(url)) {
            AtomicFiles.copy(in, target);
            return target;
        } catch (IOException e) {
            throw new HttpException("Download failed", e);
        }
    }

    private static InputStream stream(String url) {
        URI uri = HttpSupport.uri(url);
        HttpRequest request;
        try {
            request = HttpRequest.newBuilder(uri)
                    .timeout(REQUEST_TIMEOUT)
                    .GET()
                    .build();
        } catch (IllegalArgumentException e) {
            throw new HttpException("Invalid HTTP request", e);
        }
        try {
            HttpResponse<InputStream> resp = HTTP_CLIENT.send(
                    request, HttpResponse.BodyHandlers.ofInputStream());
            if (resp.statusCode() / 100 != 2) {
                IOUtils.closeQuietly(resp.body());
                throw new HttpException("Download failed: status=" + resp.statusCode());
            }
            return resp.body();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new HttpException("Download interrupted", e);
        } catch (IOException e) {
            throw new HttpException("Download failed", e);
        }
    }
}
