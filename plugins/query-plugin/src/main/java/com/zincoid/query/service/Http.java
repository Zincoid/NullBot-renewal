package com.zincoid.query.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;

/**
 * 查询插件使用的 HTTP 工具。
 * <p>HTTP helper used by the query plugin.</p>
 */
public final class Http {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(20);

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(CONNECT_TIMEOUT)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private Http() {
    }

    // ════ Get ═════════════════════

    /**
     * 发送 GET 请求。
     * <p>Sends a GET request.</p>
     *
     * @param url       request URL
     * @param apiKey    optional x-api-key value
     * @param userAgent user agent
     * @return the text response
     * @throws IOException          on transport failure
     * @throws InterruptedException when interrupted
     */
    public static HttpResponse<String> get(String url, String apiKey, String userAgent)
            throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url))
                .timeout(REQUEST_TIMEOUT)
                .header("Accept", "application/json")
                .header("User-Agent", userAgent)
                .GET();
        if (apiKey != null && !apiKey.isBlank())
            builder.header("x-api-key", apiKey);
        return CLIENT.send(builder.build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    /**
     * 发送不带 API key 的 GET 请求。
     * <p>Sends a GET request without an API key.</p>
     *
     * @param url       request URL
     * @param userAgent user agent
     * @return the text response
     * @throws IOException          on transport failure
     * @throws InterruptedException when interrupted
     */
    public static HttpResponse<String> get(String url, String userAgent)
            throws IOException, InterruptedException {
        return get(url, null, userAgent);
    }

    /**
     * 下载字节内容。
     * <p>Downloads bytes.</p>
     *
     * @param url       request URL
     * @param userAgent user agent
     * @return the response bytes
     * @throws IOException          on transport failure
     * @throws InterruptedException when interrupted
     */
    public static byte[] getBytes(String url, String userAgent)
            throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(REQUEST_TIMEOUT)
                .header("User-Agent", userAgent)
                .GET()
                .build();
        HttpResponse<byte[]> response = CLIENT.send(request,
                HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() != 200)
            throw new IOException("HTTP " + response.statusCode());
        return response.body();
    }

    // ════ Multipart ═══════════════

    /**
     * 以 multipart/form-data 上传一个文件字段。
     * <p>Uploads one file field as multipart/form-data.</p>
     *
     * @param url       request URL
     * @param userAgent user agent
     * @param filename  uploaded filename
     * @param bytes     file bytes
     * @return the text response
     * @throws IOException          on transport failure
     * @throws InterruptedException when interrupted
     */
    public static HttpResponse<String> postFile(String url, String userAgent, String filename, byte[] bytes)
            throws IOException, InterruptedException {
        String boundary = "----NullBot" + UUID.randomUUID().toString().replace("-", "");
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        write(body, "--" + boundary + "\r\n");
        write(body, "Content-Disposition: form-data; name=\"file\"; filename=\"" + filename + "\"\r\n");
        write(body, "Content-Type: application/octet-stream\r\n\r\n");
        body.write(bytes);
        write(body, "\r\n--" + boundary + "--\r\n");

        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(REQUEST_TIMEOUT)
                .header("User-Agent", userAgent)
                .header("Accept", "application/json")
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray()))
                .build();
        return CLIENT.send(request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private static void write(ByteArrayOutputStream body, String text) throws IOException {
        body.write(text.getBytes(StandardCharsets.UTF_8));
    }
}
