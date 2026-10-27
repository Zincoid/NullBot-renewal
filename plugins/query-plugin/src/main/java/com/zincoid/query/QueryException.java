package com.zincoid.query;

/**
 * 查询插件异常：接口请求或响应解析失败。
 * <p>Query plugin exception: API request or response parsing failures.</p>
 */
public class QueryException extends RuntimeException {

    public QueryException(String message) {
        super(message);
    }

    public QueryException(String message, Throwable cause) {
        super(message, cause);
    }
}
