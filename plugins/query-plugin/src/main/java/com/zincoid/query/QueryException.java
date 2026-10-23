package com.zincoid.query;

/**
 * 查询插件异常。
 * <p>Query plugin exception.</p>
 */
public class QueryException extends RuntimeException {

    public QueryException(String message) {
        super(message);
    }

    public QueryException(String message, Throwable cause) {
        super(message, cause);
    }
}
