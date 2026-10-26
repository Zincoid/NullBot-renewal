package com.zincoid.gd;

/**
 * GD 插件内部异常：网络请求、数据解析或服务未就绪。
 * <p>Internal GD plugin exception: network failures, parse failures, or an unavailable service.</p>
 */
public class GdException extends RuntimeException {

    public GdException(String message) {
        super(message);
    }

    public GdException(String message, Throwable cause) {
        super(message, cause);
    }
}
