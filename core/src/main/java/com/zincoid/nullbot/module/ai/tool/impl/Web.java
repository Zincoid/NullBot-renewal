package com.zincoid.nullbot.module.ai.tool.impl;

import com.zincoid.nullbot.module.ai.tool.Tool;
import com.zincoid.nullbot.module.ai.tool.ToolContext;
import com.zincoid.nullbot.module.ai.tool.ToolDefinition;
import com.zincoid.nullbot.module.ai.tool.ToolRes;
import com.zincoid.nullbot.platform.util.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * 网页工具：读取任意网页的文本内容，用于获取知识。
 * <p>Web tool: reads the text content of any webpage for knowledge acquisition.</p>
 */
@Slf4j
@Component
public class Web implements Tool {

    private static final int MAX_BODY_LENGTH = 8192;
    private static final int MAX_BODY_BYTES = 1024 * 1024;
    private static final int MAX_REDIRECTS = 5;
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
            .followRedirects(HttpClient.Redirect.NEVER)
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
     * 请求网页并返回响应正文，手动跟随有限次重定向。
     * <p>Requests the webpage and returns the response body, following a limited number of redirects manually.</p>
     *
     * @param url the webpage URL
     * @return the response body
     * @throws Exception when the request fails or the response is unsupported
     */
    private String fetchPage(String url) throws Exception {
        URI uri = URI.create(url);
        for (int redirects = 0; redirects <= MAX_REDIRECTS; redirects++) {
            validate(uri);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .header("Accept-Language", "zh-CN,zh;q=0.9")
                    .timeout(Duration.ofSeconds(15))
                    .GET()
                    .build();
            HttpResponse<InputStream> response;
            try {
                response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw e;
            }
            int status = response.statusCode();
            if (status == 301 || status == 302 || status == 303 || status == 307 || status == 308) {
                String location = response.headers().firstValue("Location").orElse(null);
                response.body().close();
                if (location == null || location.isBlank())
                    throw new RuntimeException("Redirect without Location");
                uri = uri.resolve(location);
                continue;
            }
            String contentType = response.headers().firstValue("Content-Type").orElse("");
            if (status != 200) {
                response.body().close();
                throw new RuntimeException("HTTP %d".formatted(status));
            }
            String type = contentType.toLowerCase(Locale.ROOT);
            if (!type.contains("text/html") && !type.contains("text/plain")) {
                response.body().close();
                throw new RuntimeException("Unsupported Content-Type: %s, only HTML/text pages are supported".formatted(contentType));
            }
            try (InputStream body = response.body()) {
                return new String(body.readNBytes(MAX_BODY_BYTES), charset(contentType));
            }
        }
        throw new RuntimeException("Too many redirects: " + MAX_REDIRECTS);
    }

    /**
     * 校验请求地址，拒绝非 http/https 与本机、内网目标。
     * <p>Validates the request URI, rejecting non-http/https targets and local or private hosts.</p>
     *
     * @param uri the request URI
     * @throws Exception when the host cannot be resolved
     */
    private static void validate(URI uri) throws Exception {
        String scheme = uri.getScheme();
        if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https")))
            throw new RuntimeException("Unsupported URL scheme, only http/https is allowed");
        String host = uri.getHost();
        if (host == null || host.isBlank())
            throw new RuntimeException("Missing URL host");
        if (host.startsWith("[") && host.endsWith("]"))
            host = host.substring(1, host.length() - 1);
        for (InetAddress address : InetAddress.getAllByName(host)) {
            byte[] bytes = address.getAddress();
            if (address.isAnyLocalAddress() || address.isLoopbackAddress()
                    || address.isLinkLocalAddress() || address.isSiteLocalAddress()
                    || address.isMulticastAddress()
                    || (bytes.length == 16 && (bytes[0] & 0xFE) == 0xFC))
                throw new RuntimeException("Blocked URL host: " + host);
        }
    }

    /**
     * 读取响应字符集，缺失或非法时回退 UTF-8。
     * <p>Reads the response charset, falling back to UTF-8 when missing or invalid.</p>
     *
     * @param contentType the Content-Type header value
     * @return the response charset
     */
    private static Charset charset(String contentType) {
        try {
            Charset charset = MediaType.parseMediaType(contentType).getCharset();
            return charset == null ? StandardCharsets.UTF_8 : charset;
        } catch (InvalidMediaTypeException e) {
            return StandardCharsets.UTF_8;
        }
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
