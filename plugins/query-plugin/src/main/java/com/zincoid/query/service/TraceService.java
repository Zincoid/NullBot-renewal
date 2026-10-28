package com.zincoid.query.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.zincoid.nullbot.module.http.HttpService;
import com.zincoid.nullbot.platform.util.JsonUtils;
import com.zincoid.query.QueryException;
import com.zincoid.query.config.QueryConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 图片溯源服务：调用 SauceNAO 搜索接口。
 * <p>Image trace service: calls the SauceNAO search API.</p>
 */
@Slf4j
@RequiredArgsConstructor
public class TraceService {

    private static final String API_URL = "https://saucenao.com/search.php";

    private final QueryConfig config;
    private final QueryHttp http;

    /**
     * 按图片搜索出处。
     * <p>Searches for the source of an image.</p>
     *
     * @param imageSource image source
     * @param count       maximum result count
     * @return the matched results
     */
    public List<TraceResult> search(String imageSource, int count) {
        byte[] image = QueryImages.load(imageSource, http);
        String url = API_URL + "?output_type=2&db=999&numres=" + count;
        if (!config.sauceNaoApiKey().isBlank())
            url += "&api_key=" + URLEncoder.encode(config.sauceNaoApiKey(), StandardCharsets.UTF_8);

        HttpService.Response response = http.postFile(url, "image.jpg", image);
        if (response.status() != 200)
            throw new QueryException("SauceNAO HTTP " + response.status());

        TraceResponse body = JsonUtils.parse(response.text(), TraceResponse.class);
        if (body == null || body.header() == null || body.header().status() == null)
            throw new QueryException("SauceNAO returned an invalid response");
        if (body.header().status() != 0)
            throw new QueryException("SauceNAO status " + body.header().status());
        if (body.results() == null || body.results().isEmpty()) return List.of();

        List<TraceResult> results = new ArrayList<>();
        for (TraceItem item : body.results()) {
            if (item == null || item.header() == null || item.data() == null) continue;
            TraceItemHeader header = item.header();
            TraceData data = item.data();
            results.add(new TraceResult(
                    text(header.similarity()),
                    text(header.indexName()),
                    titleOf(data),
                    authorOf(data),
                    firstOf(data.extUrls()),
                    text(header.thumbnail())
            ));
        }
        log.info("SauceNAO results: {}", results.size());
        return results;
    }

    // ════ Helpers ═════════════════

    private static String titleOf(TraceData data) {
        String title = firstOf(data.title());
        return title == null || title.isBlank() ? "未知" : title;
    }

    private static String authorOf(TraceData data) {
        for (Object value : new Object[]{data.memberName(), data.author(), data.creator()}) {
            String name = firstOf(value);
            if (name != null && !name.isBlank()) return name;
        }
        return "未知";
    }

    private static String firstOf(Object value) {
        return switch (value) {
            case null -> null;
            case String text -> text;
            case List<?> list when !list.isEmpty() && list.getFirst() != null -> list.getFirst().toString();
            default -> value.toString();
        };
    }

    private static String text(String value) {
        return value == null ? "" : value;
    }

    // ════ Models ══════════════════

    /**
     * 单条溯源结果。
     * <p>One trace result.</p>
     *
     * @param similarity similarity percentage
     * @param source     source index name
     * @param title      work title
     * @param author     author name
     * @param url        source URL
     * @param thumbnail  thumbnail URL
     */
    public record TraceResult(
            String similarity,
            String source,
            String title,
            String author,
            String url,
            String thumbnail
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record TraceResponse(TraceHeader header, List<TraceItem> results) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record TraceHeader(Integer status) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record TraceItem(TraceItemHeader header, TraceData data) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record TraceItemHeader(
            String similarity,
            String thumbnail,
            @JsonProperty("index_name") String indexName
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record TraceData(
            Object title,
            @JsonProperty("member_name") Object memberName,
            Object author,
            Object creator,
            @JsonProperty("ext_urls") Object extUrls
    ) {
    }
}
