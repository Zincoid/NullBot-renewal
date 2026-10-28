package com.zincoid.sb6657.service;

import com.zincoid.nullbot.module.http.HttpException;
import com.zincoid.nullbot.module.http.HttpService;
import com.zincoid.sb6657.SB6657Exception;
import lombok.RequiredArgsConstructor;

import java.time.Duration;

/**
 * SB6657 HTTP 适配器：绑定接口地址、超时与 JSON 请求头。
 * <p>SB6657 HTTP adapter binding the API URL, timeouts, and JSON headers.</p>
 */
@RequiredArgsConstructor
public final class SB6657Http {

    private static final String API_URL = "https://api.hguofichp.cn/machine/getRandOne";
    private static final HttpService.Options OPTIONS = HttpService.Options.defaults()
            .withConnectTimeout(Duration.ofSeconds(10))
            .withRequestTimeout(Duration.ofSeconds(15))
            .withMaxBytes(1024 * 1024)
            .withHeader("Accept", "application/json");

    private final HttpService http;

    /**
     * 获取一条随机烂梗。
     * <p>Fetches one random meme.</p>
     *
     * @return the response
     */
    public HttpService.Response get() {
        try {
            return http.get(API_URL, OPTIONS);
        } catch (HttpException e) {
            throw new SB6657Exception("Request failed", e);
        }
    }
}
