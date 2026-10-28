package com.zincoid.nullbot.module.http;

/**
 * HTTP 请求失败：网络、URL、代理、响应大小或中断。
 * <p>HTTP request failure: network, URL, proxy, response size, or interruption.</p>
 */
public class HttpException extends RuntimeException {

    public HttpException(String message) {
        super(message);
    }

    public HttpException(String message, Throwable cause) {
        super(message, cause);
    }
}
