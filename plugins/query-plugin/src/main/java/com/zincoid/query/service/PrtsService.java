package com.zincoid.query.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.zincoid.nullbot.platform.util.JsonUtils;
import com.zincoid.query.QueryException;
import com.zincoid.query.config.QueryConfig;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.net.URLEncoder;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * PRTS 查询服务：调用 MediaWiki API 并缓存页面 HTML。
 * <p>PRTS query service: calls the MediaWiki API and caches page HTML.</p>
 */
@Slf4j
public class PrtsService {

    private static final String API_URL = "https://prts.wiki/api.php";

    private final QueryConfig config;
    private final Map<String, Cached> cache = new ConcurrentHashMap<>();

    public PrtsService(QueryConfig config) {
        this.config = config;
    }

    /**
     * 获取并解析 PRTS 页面。
     * <p>Fetches and parses a PRTS page.</p>
     *
     * @param keyword page title
     * @return the parsed page
     */
    public Document document(String keyword) {
        return Jsoup.parse(html(keyword));
    }

    private String html(String keyword) {
        Cached cached = cache.get(keyword);
        if (cached != null && cached.fresh(config.prtsCacheTtl())) return cached.html();
        String url = API_URL
                + "?action=parse&page=" + URLEncoder.encode(keyword, StandardCharsets.UTF_8)
                + "&prop=text&format=json&formatversion=2";
        HttpResponse<String> response;
        try {
            response = Http.get(url, config.userAgent());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new QueryException("PRTS request interrupted", e);
        } catch (IOException e) {
            throw new QueryException("PRTS request failed", e);
        }
        if (response.statusCode() != 200)
            throw new QueryException("PRTS HTTP " + response.statusCode());
        PrtsResponse body = JsonUtils.parse(response.body(), PrtsResponse.class);
        if (body == null || body.parse() == null || body.parse().text() == null
                || body.parse().text().isBlank())
            throw new QueryException("PRTS page not found: " + keyword);
        String html = body.parse().text();
        cache.put(keyword, new Cached(Instant.now(), html));
        log.info("PRTS fetched: {}, chars={}", keyword, html.length());
        return html;
    }

    private record Cached(Instant fetchedAt, String html) {
        boolean fresh(Duration ttl) {
            return Duration.between(fetchedAt, Instant.now()).compareTo(ttl) < 0;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record PrtsResponse(Parse parse) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Parse(String title, String text) {
    }
}
