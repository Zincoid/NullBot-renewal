package com.zincoid.nullbot.module.http;

import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * JDK HTTP 客户端的共享支持：URL 校验、客户端构建与复用。
 * <p>Shared support for JDK HTTP clients: URL validation, client construction, and reuse.</p>
 */
final class HttpSupport {

    private static final Map<ClientKey, HttpClient> CLIENTS = new ConcurrentHashMap<>();

    private HttpSupport() {
    }

    /**
     * 解析并校验 HTTP(S) URL。
     * <p>Parses and validates an HTTP(S) URL.</p>
     *
     * @param url the request URL
     * @return the parsed URI
     */
    static URI uri(String url) {
        if (url == null || url.isBlank())
            throw new HttpException("Missing URL");
        URI uri;
        try {
            uri = URI.create(url);
        } catch (IllegalArgumentException e) {
            throw new HttpException("Malformed URL", e);
        }
        String scheme = uri.getScheme();
        if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme))
            throw new HttpException("Unsupported URL scheme: " + scheme);
        return uri;
    }

    /**
     * 返回共享的 HTTP 客户端，按代理、连接超时与 HTTP 版本复用。
     * <p>Returns a shared HTTP client, reused by proxy, connect timeout, and HTTP version.</p>
     *
     * @param proxy          proxy address, blank for none
     * @param connectTimeout connect timeout
     * @param http11         whether to force HTTP/1.1
     * @return the HTTP client
     */
    static HttpClient client(String proxy, Duration connectTimeout, boolean http11) {
        ClientKey key = new ClientKey(proxy == null ? "" : proxy, connectTimeout, http11);
        return CLIENTS.computeIfAbsent(key, HttpSupport::build);
    }

    private static HttpClient build(ClientKey key) {
        HttpClient.Builder builder = HttpClient.newBuilder()
                .connectTimeout(key.connectTimeout())
                .followRedirects(HttpClient.Redirect.NORMAL);
        if (key.http11()) builder.version(HttpClient.Version.HTTP_1_1);
        if (!key.proxy().isBlank()) builder.proxy(proxy(key.proxy()));
        return builder.build();
    }

    private static ProxySelector proxy(String raw) {
        ProxyAddress address = ProxyAddress.parse(raw);
        return ProxySelector.of(InetSocketAddress.createUnresolved(
                address.host(), address.port()));
    }

    private record ClientKey(String proxy, Duration connectTimeout, boolean http11) {
    }
}
