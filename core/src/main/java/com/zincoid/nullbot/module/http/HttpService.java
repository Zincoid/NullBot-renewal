package com.zincoid.nullbot.module.http;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 宿主通用 HTTP 服务。
 * <p>Host HTTP service.</p>
 * <p>调用是同步的；非 2xx 响应不会抛异常，而是通过 {@link Response#status()} 返回。</p>
 * <p>Calls are synchronous; non-2xx responses are returned through {@link Response#status()} instead of throwing.</p>
 */
public interface HttpService {

    /**
     * 发送 GET 请求。
     * <p>Sends a GET request.</p>
     *
     * @param url     the request URL
     * @param options the request options
     * @return the response
     */
    Response get(String url, Options options);

    /**
     * 发送带字节请求体的 POST 请求。
     * <p>Sends a POST request with a byte body.</p>
     *
     * @param url         the request URL
     * @param body        the request body
     * @param contentType the content type, null or blank for application/octet-stream
     * @param options     the request options
     * @return the response
     */
    Response post(String url, byte[] body, String contentType, Options options);

    /**
     * 以 multipart/form-data 上传单个文件字段。
     * <p>Uploads a single file field as multipart/form-data.</p>
     *
     * @param url       the request URL
     * @param fieldName the form field name
     * @param filename  the uploaded filename
     * @param content   the file content
     * @param options   the request options
     * @return the response
     */
    Response upload(String url, String fieldName, String filename, byte[] content, Options options);

    /**
     * HTTP 响应。
     * <p>An HTTP response.</p>
     *
     * @param status      the HTTP status code
     * @param body        the response body
     * @param contentType the content type header, never null
     */
    record Response(int status, byte[] body, String contentType) {

        /**
         * 创建响应，空值归一化为空数组或空字符串。
         * <p>Creates a response, normalizing nulls to an empty array or string.</p>
         */
        public Response {
            body = body == null ? new byte[0] : body;
            contentType = contentType == null ? "" : contentType;
        }

        /**
         * 是否返回 2xx。
         * <p>Whether the status is 2xx.</p>
         *
         * @return true for a 2xx status
         */
        public boolean ok() {
            return status / 100 == 2;
        }

        /**
         * 以 UTF-8 解码响应体。
         * <p>Decodes the body as UTF-8.</p>
         *
         * @return the decoded text
         */
        public String text() {
            return new String(body, StandardCharsets.UTF_8);
        }
    }

    /**
     * HTTP 请求选项，不可变；通过 {@link #defaults()} 和 {@code with*} 方法构建。
     * <p>Immutable HTTP request options built through {@link #defaults()} and {@code with*} methods.</p>
     */
    final class Options {

        private static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(10);
        private static final Duration DEFAULT_REQUEST_TIMEOUT = Duration.ofSeconds(20);
        private static final int DEFAULT_MAX_BYTES = 8 * 1024 * 1024;
        private static final int MAX_RETRIES = 5;

        private static final Options DEFAULTS = new Options(
                Map.of(),
                DEFAULT_CONNECT_TIMEOUT,
                DEFAULT_REQUEST_TIMEOUT,
                0,
                DEFAULT_MAX_BYTES,
                ""
        );

        private final Map<String, String> headers;
        private final Duration connectTimeout;
        private final Duration requestTimeout;
        private final int retries;
        private final int maxBytes;
        private final String proxy;

        private Options(Map<String, String> headers, Duration connectTimeout,
                        Duration requestTimeout, int retries, int maxBytes, String proxy) {
            this.headers = headers == null ? Map.of() : Map.copyOf(headers);
            this.connectTimeout = positive(connectTimeout, "connect timeout");
            this.requestTimeout = positive(requestTimeout, "request timeout");
            if (retries < 0 || retries > MAX_RETRIES)
                throw new IllegalArgumentException("Retries must be 0-" + MAX_RETRIES + ": " + retries);
            if (maxBytes < 1 || maxBytes == Integer.MAX_VALUE)
                throw new IllegalArgumentException("Invalid max bytes: " + maxBytes);
            this.retries = retries;
            this.maxBytes = maxBytes;
            this.proxy = proxy == null ? "" : proxy;
        }

        /**
         * 返回默认选项。
         * <p>Returns the default options.</p>
         *
         * @return the default options
         */
        public static Options defaults() {
            return DEFAULTS;
        }

        /**
         * 返回请求头。
         * <p>Returns the request headers.</p>
         *
         * @return the immutable request headers
         */
        public Map<String, String> headers() {
            return headers;
        }

        /**
         * 返回连接超时。
         * <p>Returns the connect timeout.</p>
         *
         * @return the connect timeout
         */
        public Duration connectTimeout() {
            return connectTimeout;
        }

        /**
         * 返回请求超时。
         * <p>Returns the request timeout.</p>
         *
         * @return the request timeout
         */
        public Duration requestTimeout() {
            return requestTimeout;
        }

        /**
         * 返回重试次数。
         * <p>Returns the retry count.</p>
         *
         * @return the retry count
         */
        public int retries() {
            return retries;
        }

        /**
         * 返回响应体上限。
         * <p>Returns the response body limit.</p>
         *
         * @return the maximum response body bytes
         */
        public int maxBytes() {
            return maxBytes;
        }

        /**
         * 返回代理地址。
         * <p>Returns the proxy address.</p>
         *
         * @return the proxy address, blank when unused
         */
        public String proxy() {
            return proxy;
        }

        /**
         * 返回添加或替换一个请求头后的选项。
         * <p>Returns options with one header added or replaced.</p>
         *
         * @param name  the header name
         * @param value the header value
         * @return the new options
         */
        public Options withHeader(String name, String value) {
            if (name == null || name.isBlank())
                throw new IllegalArgumentException("Blank header name");
            if (value == null)
                throw new IllegalArgumentException("Null header value: " + name);
            Map<String, String> copy = new LinkedHashMap<>(headers);
            copy.put(name, value);
            return new Options(copy, connectTimeout, requestTimeout, retries, maxBytes, proxy);
        }

        /**
         * 返回合并请求头后的选项。
         * <p>Returns options with the headers merged.</p>
         *
         * @param values the headers to merge
         * @return the new options
         */
        public Options withHeaders(Map<String, String> values) {
            if (values == null || values.isEmpty()) return this;
            Map<String, String> copy = new LinkedHashMap<>(headers);
            values.forEach((name, value) -> {
                if (name == null || name.isBlank())
                    throw new IllegalArgumentException("Blank header name");
                if (value == null)
                    throw new IllegalArgumentException("Null header value: " + name);
                copy.put(name, value);
            });
            return new Options(copy, connectTimeout, requestTimeout, retries, maxBytes, proxy);
        }

        /**
         * 返回使用指定连接超时的选项。
         * <p>Returns options with the given connect timeout.</p>
         *
         * @param timeout the connect timeout
         * @return the new options
         */
        public Options withConnectTimeout(Duration timeout) {
            return new Options(headers, timeout, requestTimeout, retries, maxBytes, proxy);
        }

        /**
         * 返回使用指定请求超时的选项。
         * <p>Returns options with the given request timeout.</p>
         *
         * @param timeout the request timeout
         * @return the new options
         */
        public Options withRequestTimeout(Duration timeout) {
            return new Options(headers, connectTimeout, timeout, retries, maxBytes, proxy);
        }

        /**
         * 返回使用指定重试次数的选项。
         * <p>Returns options with the given retry count.</p>
         *
         * @param retries the retry count
         * @return the new options
         */
        public Options withRetries(int retries) {
            return new Options(headers, connectTimeout, requestTimeout, retries, maxBytes, proxy);
        }

        /**
         * 返回使用指定响应体上限的选项。
         * <p>Returns options with the given response body limit.</p>
         *
         * @param maxBytes the maximum response body bytes
         * @return the new options
         */
        public Options withMaxBytes(int maxBytes) {
            return new Options(headers, connectTimeout, requestTimeout, retries, maxBytes, proxy);
        }

        /**
         * 返回使用指定代理的选项，空白表示不使用代理。
         * <p>Returns options with the given proxy; blank disables the proxy.</p>
         *
         * @param proxy the proxy address
         * @return the new options
         */
        public Options withProxy(String proxy) {
            String value = proxy == null || proxy.isBlank()
                    ? "" : ProxyAddress.parse(proxy).normalized();
            return new Options(headers, connectTimeout, requestTimeout, retries, maxBytes, value);
        }

        // ════ Helpers ═════════════════

        private static Duration positive(Duration value, String name) {
            if (value == null || value.isZero() || value.isNegative())
                throw new IllegalArgumentException("Invalid " + name + ": " + value);
            return value;
        }
    }
}
