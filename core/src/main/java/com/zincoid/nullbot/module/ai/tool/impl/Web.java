package com.zincoid.nullbot.module.ai.tool.impl;

import com.zincoid.nullbot.module.ai.tool.Tool;
import com.zincoid.nullbot.module.ai.tool.ToolContext;
import com.zincoid.nullbot.module.ai.tool.ToolDefinition;
import com.zincoid.nullbot.module.ai.tool.ToolRes;
import com.zincoid.nullbot.platform.util.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Pattern;

/**
 * 网页工具：读取任意网页的文本内容，用于获取知识。
 * <p>Web tool: reads the text content of any webpage for knowledge acquisition.</p>
 */
@Slf4j
@Component
public class Web implements Tool {

    private static final int MAX_BODY_LENGTH = 8192;
    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s{3,}");
    private static final Pattern[] STRIP_PATTERNS = {
            Pattern.compile("<script[^>]*>.*?</script>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE),
            Pattern.compile("<style[^>]*>.*?</style>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE),
            Pattern.compile("<head[^>]*>.*?</head>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE),
            Pattern.compile("<!--.*?-->", Pattern.DOTALL),
            Pattern.compile("<[^>]+>")
    };

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .build();

    // ════ Tool ════════════════════

    @Override
    public ToolDefinition definition() {
        return ToolDefinition.builder("web", """
                        Reads any webpage content for knowledge acquisition.""")
                .addString("url", "web url link", true)
                .build();
    }

    @Override
    public ToolRes run(ToolContext ctx, String args) {
        Args _args = JsonUtils.parse(args, Args.class);
        if (_args.url == null || _args.url.isBlank())
            return ToolRes.error("URL must not be empty");
        if (!_args.url.startsWith("http://") && !_args.url.startsWith("https://"))
            return ToolRes.error("Unsupported URL scheme, only http/https is allowed");
        try {
            String text = extractText(fetchPage(_args.url));
            return ToolRes.of("Page content (%s):\n%s".formatted(_args.url, text));
        } catch (Exception e) {
            log.warn("Web fetch failed: {}", e.getMessage());
            return ToolRes.error("Failed to fetch webpage: " + e.getMessage());
        }
    }

    // ════ Helpers ═════════════════

    /**
     * 请求网页并返回响应正文。
     * <p>Requests the webpage and returns the response body.</p>
     *
     * @param url the webpage URL
     * @return the response body
     * @throws Exception when the request fails or the response is unsupported
     */
    private String fetchPage(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "zh-CN,zh;q=0.9")
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        String contentType = response.headers().firstValue("Content-Type").orElse("");
        if (!contentType.contains("text/html") && !contentType.contains("text/plain"))
            throw new RuntimeException("Unsupported Content-Type: %s, only HTML/text pages are supported".formatted(contentType));
        if (response.statusCode() != 200)
            throw new RuntimeException("HTTP %d".formatted(response.statusCode()));
        return response.body();
    }

    /**
     * 去除标签与注释并压缩空白，超长时截断。
     * <p>Strips tags and comments, collapses whitespace, and truncates when too long.</p>
     *
     * @param html the raw HTML
     * @return the extracted text
     */
    private static String extractText(String html) {
        String text = html;
        for (Pattern p : STRIP_PATTERNS)
            text = p.matcher(text).replaceAll("");
        text = text.replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'");
        text = WHITESPACE_PATTERN.matcher(text).replaceAll("\n").trim();
        if (text.length() > MAX_BODY_LENGTH)
            text = "%s...(content truncated)".formatted(text.substring(0, MAX_BODY_LENGTH));
        return text;
    }

    private record Args(String url) {
    }
}
