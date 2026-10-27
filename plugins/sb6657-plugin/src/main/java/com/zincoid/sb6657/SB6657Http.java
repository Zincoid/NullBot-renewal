package com.zincoid.sb6657;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * SB6657 插件使用的 HTTP 工具。
 * <p>HTTP helper used by the SB6657 plugin.</p>
 */
final class SB6657Http {

    private static final String API_URL = "https://api.hguofichp.cn/machine/getRandOne";
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(15);

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(CONNECT_TIMEOUT)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private SB6657Http() {
    }

    /**
     * 请求随机烂梗接口。
     * <p>Requests the random meme API.</p>
     *
     * @return the text response
     * @throws IOException          on transport failure
     * @throws InterruptedException when interrupted
     */
    static HttpResponse<String> get() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(API_URL))
                .timeout(REQUEST_TIMEOUT)
                .header("Accept", "application/json")
                .GET()
                .build();
        return CLIENT.send(request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }
}
