package com.zincoid.sb6657;

/**
 * SB6657 插件异常：接口请求或响应解析失败。
 * <p>SB6657 plugin exception: API request or response parsing failures.</p>
 */
public class SB6657Exception extends RuntimeException {

    public SB6657Exception(String message) {
        super(message);
    }

    public SB6657Exception(String message, Throwable cause) {
        super(message, cause);
    }
}
