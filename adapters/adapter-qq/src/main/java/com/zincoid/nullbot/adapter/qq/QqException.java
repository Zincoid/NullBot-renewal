package com.zincoid.nullbot.adapter.qq;

import com.zincoid.nullbot.bot.spi.port.SpiException;

/**
 * QQ 官方机器人调用异常。
 * <p>QQ official bot call exception.</p>
 */
public class QqException extends SpiException {

    private final int status;
    private final int code;

    /**
     * 创建不带状态码的异常。
     * <p>Creates an exception without a status or API code.</p>
     *
     * @param message the error message
     */
    public QqException(String message) {
        this(0, 0, message, null);
    }

    /**
     * 创建带原因的异常。
     * <p>Creates an exception with a cause.</p>
     *
     * @param message the error message
     * @param cause   the cause
     */
    public QqException(String message, Throwable cause) {
        this(0, 0, message, cause);
    }

    /**
     * 创建带 HTTP 状态与业务码的异常。
     * <p>Creates an exception with an HTTP status and API code.</p>
     *
     * @param status  the HTTP status
     * @param code    the API error code
     * @param message the error message
     */
    public QqException(int status, int code, String message) {
        this(status, code, message, null);
    }

    /**
     * 创建带完整信息的异常。
     * <p>Creates an exception with full details.</p>
     *
     * @param status  the HTTP status
     * @param code    the API error code
     * @param message the error message
     * @param cause   the cause
     */
    public QqException(int status, int code, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
        this.code = code;
    }

    /**
     * HTTP 状态码，未知时为 0。
     * <p>HTTP status code, or zero when unknown.</p>
     *
     * @return the HTTP status
     */
    public int status() {
        return status;
    }

    /**
     * 开放平台业务错误码，未知时为 0。
     * <p>Open platform error code, or zero when unknown.</p>
     *
     * @return the API error code
     */
    public int code() {
        return code;
    }
}
