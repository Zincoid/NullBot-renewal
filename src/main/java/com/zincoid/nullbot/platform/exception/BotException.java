package com.zincoid.nullbot.platform.exception;

/**
 * Bot-global base of the exception family.
 * <p>机器人全局异常基类。</p>
 */
public abstract class BotException extends RuntimeException {

    protected BotException(String message) {
        super(message);
    }

    protected BotException(String message, Throwable cause) {
        super(message, cause);
    }
}
