package com.zincoid.query.service;

import com.zincoid.nullbot.module.http.HttpException;
import com.zincoid.nullbot.module.http.HttpService;
import com.zincoid.query.QueryException;
import com.zincoid.query.config.QueryConfig;

import java.time.Duration;
import java.util.function.Supplier;

/**
 * 查询插件 HTTP 适配器：统一请求头、超时与错误映射。
 * <p>HTTP adapter for the query plugin: centralizes headers, timeouts, and error mapping.</p>
 */
public final class QueryHttp {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(20);
    private static final int MAX_JSON_BYTES = 8 * 1024 * 1024;
    private static final int MAX_IMAGE_BYTES = 32 * 1024 * 1024;

    private final HttpService http;
    private final HttpService.Options base;

    /**
     * 创建 HTTP 适配器。
     * <p>Creates the HTTP adapter.</p>
     *
     * @param http   the host HTTP service
     * @param config the plugin configuration
     */
    public QueryHttp(HttpService http, QueryConfig config) {
        this.http = http;
        this.base = HttpService.Options.defaults()
                .withConnectTimeout(CONNECT_TIMEOUT)
                .withRequestTimeout(REQUEST_TIMEOUT)
                .withMaxBytes(MAX_JSON_BYTES)
                .withHeader("User-Agent", config.userAgent());
    }

    /**
     * 发送 JSON GET 请求。
     * <p>Sends a JSON GET request.</p>
     *
     * @param url    the request URL
     * @param apiKey the optional x-api-key value
     * @return the response
     */
    public HttpService.Response get(String url, String apiKey) {
        HttpService.Options options = base.withHeader("Accept", "application/json");
        if (apiKey != null && !apiKey.isBlank())
            options = options.withHeader("x-api-key", apiKey);
        HttpService.Options finalOptions = options;
        return send(() -> http.get(url, finalOptions));
    }

    /**
     * 发送不带 API key 的 JSON GET 请求。
     * <p>Sends a JSON GET request without an API key.</p>
     *
     * @param url the request URL
     * @return the response
     */
    public HttpService.Response get(String url) {
        return get(url, null);
    }

    /**
     * 下载字节内容，要求返回 200。
     * <p>Downloads bytes and requires a 200 status.</p>
     *
     * @param url the request URL
     * @return the response
     */
    public HttpService.Response getBytes(String url) {
        HttpService.Options options = base.withMaxBytes(MAX_IMAGE_BYTES);
        HttpService.Response response = send(() -> http.get(url, options));
        if (response.status() != 200)
            throw new QueryException("HTTP " + response.status());
        return response;
    }

    /**
     * 以 multipart/form-data 上传一个文件字段。
     * <p>Uploads one file field as multipart/form-data.</p>
     *
     * @param url      the request URL
     * @param filename the uploaded filename
     * @param bytes    the file bytes
     * @return the response
     */
    public HttpService.Response postFile(String url, String filename, byte[] bytes) {
        HttpService.Options options = base.withHeader("Accept", "application/json");
        return send(() -> http.upload(url, "file", filename, bytes, options));
    }

    // ════ Helpers ═════════════════

    private static HttpService.Response send(Supplier<HttpService.Response> request) {
        try {
            return request.get();
        } catch (HttpException e) {
            throw new QueryException("HTTP request failed", e);
        }
    }
}
