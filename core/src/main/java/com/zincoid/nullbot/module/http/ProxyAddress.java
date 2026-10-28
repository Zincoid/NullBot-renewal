package com.zincoid.nullbot.module.http;

/**
 * HTTP 代理地址。
 * <p>An HTTP proxy address.</p>
 *
 * @param host the proxy host
 * @param port the proxy port
 */
record ProxyAddress(String host, int port) {

    /**
     * 解析 {@code host:port}、{@code http://host:port} 或 IPv6 形式。
     * <p>Parses {@code host:port}, {@code http://host:port}, or IPv6 forms.</p>
     *
     * @param raw the raw proxy address
     * @return the parsed proxy address
     */
    static ProxyAddress parse(String raw) {
        String value = raw.trim();
        if (value.startsWith("http://")) value = value.substring("http://".length());
        else if (value.startsWith("https://")) value = value.substring("https://".length());
        int split = value.lastIndexOf(':');
        if (split <= 0 || split == value.length() - 1)
            throw new IllegalArgumentException("Invalid proxy: " + raw);
        String host = value.substring(0, split);
        if (host.startsWith("[") && host.endsWith("]"))
            host = host.substring(1, host.length() - 1);
        if (host.isBlank()) throw new IllegalArgumentException("Invalid proxy: " + raw);
        int port;
        try {
            port = Integer.parseInt(value.substring(split + 1));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid proxy: " + raw, e);
        }
        if (port < 1 || port > 65535)
            throw new IllegalArgumentException("Invalid proxy: " + raw);
        return new ProxyAddress(host, port);
    }

    /**
     * 返回规范化的代理地址。
     * <p>Returns the normalized proxy address.</p>
     *
     * @return the normalized address
     */
    String normalized() {
        return host.indexOf(':') >= 0 ? "[" + host + "]:" + port : host + ":" + port;
    }
}
