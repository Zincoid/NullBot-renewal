package com.zincoid.gd;

/**
 * GD 插件异常：网络请求或数据解析失败。
 * <p>GD plugin exception: network or data parsing failures.</p>
 */
public class GdException extends RuntimeException {

    public GdException(String message) {
        super(message);
    }

    public GdException(String message, Throwable cause) {
        super(message, cause);
    }
}
