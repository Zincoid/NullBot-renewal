package com.zincoid.nullbot.platform.exception;

/**
 * NullBot 全局异常基类。
 * <p>NullBot-global base of the exception family.</p>
 */
public abstract class NullBotException extends RuntimeException {

    protected NullBotException(String message) {
        super(message);
    }

    protected NullBotException(String message, Throwable cause) {
        super(message, cause);
    }
}
