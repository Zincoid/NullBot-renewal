package com.zincoid.say.service;

import com.zincoid.nullbot.module.http.HttpService;
import lombok.RequiredArgsConstructor;

/**
 * 语录插件 HTTP 适配器：绑定头像下载的大小限制。
 * <p>Say HTTP adapter binding the avatar download size limit.</p>
 */
@RequiredArgsConstructor
public final class SayHttp {

    private static final HttpService.Options OPTIONS = HttpService.Options.defaults()
            .withMaxBytes(5 * 1024 * 1024);

    private final HttpService http;

    /**
     * 获取头像。
     * <p>Fetches an avatar.</p>
     *
     * @param url the avatar URL
     * @return the response
     */
    public HttpService.Response get(String url) {
        return http.get(url, OPTIONS);
    }
}
