package com.zincoid.symmetry.service;

import com.zincoid.nullbot.module.http.HttpException;
import com.zincoid.nullbot.module.http.HttpService;
import com.zincoid.symmetry.SymmetryException;
import lombok.RequiredArgsConstructor;

import java.time.Duration;

/**
 * 对称插件 HTTP 适配器：绑定图片下载的超时、UA 与大小限制。
 * <p>Symmetry HTTP adapter binding image download timeouts, user agent, and size limits.</p>
 */
@RequiredArgsConstructor
public final class SymmetryHttp {

    private static final HttpService.Options OPTIONS = HttpService.Options.defaults()
            .withConnectTimeout(Duration.ofSeconds(10))
            .withRequestTimeout(Duration.ofSeconds(20))
            .withMaxBytes(32 * 1024 * 1024)
            .withHeader("User-Agent", "NullBot-Symmetry/1.0");

    private final HttpService http;

    /**
     * 下载图片字节。
     * <p>Downloads image bytes.</p>
     *
     * @param url the image URL
     * @return the response
     */
    public HttpService.Response get(String url) {
        try {
            return http.get(url, OPTIONS);
        } catch (HttpException e) {
            throw new SymmetryException("Image download failed", e);
        }
    }
}
