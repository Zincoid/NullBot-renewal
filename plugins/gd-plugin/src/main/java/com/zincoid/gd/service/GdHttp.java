package com.zincoid.gd.service;

import com.zincoid.gd.GdException;
import com.zincoid.gd.config.GdConfig;
import com.zincoid.nullbot.module.http.HttpException;
import com.zincoid.nullbot.module.http.HttpService;

/**
 * GD 插件 HTTP 适配器：绑定 GD 配置与宿主 HTTP 服务。
 * <p>HTTP adapter for the GD plugin binding GD configuration to the host HTTP service.</p>
 */
public final class GdHttp {

    private final HttpService http;
    private final HttpService.Options base;

    /**
     * 创建 HTTP 适配器。
     * <p>Creates the HTTP adapter.</p>
     *
     * @param http   the host HTTP service
     * @param config the plugin configuration
     */
    public GdHttp(HttpService http, GdConfig config) {
        this.http = http;
        HttpService.Options options = HttpService.Options.defaults()
                .withConnectTimeout(config.connectTimeout())
                .withRequestTimeout(config.requestTimeout())
                .withRetries(config.retries())
                .withHeader("User-Agent", config.userAgent());
        try {
            options = options.withProxy(config.proxy());
        } catch (IllegalArgumentException e) {
            throw new GdException("Invalid proxy", e);
        }
        this.base = options;
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
    HttpService.Response get(String url, String accept, int maxBytes) {
        HttpService.Options options = base.withMaxBytes(maxBytes);
        if (accept != null && !accept.isBlank())
            options = options.withHeader("Accept", accept);
        try {
            return http.get(url, options);
        } catch (HttpException e) {
            throw new GdException("HTTP request failed", e);
        }
    }
}
