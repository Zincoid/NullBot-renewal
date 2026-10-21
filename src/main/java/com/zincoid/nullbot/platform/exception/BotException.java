package com.zincoid.nullbot.platform.exception;

/**
 * 机器人全局异常基类。
 * <p>Bot-global base of the exception family.</p>
 */
public abstract class BotException extends RuntimeException {

    protected BotException(String message) {
        super(message);
    }

    protected BotException(String message, Throwable cause) {
        super(message, cause);
    }
}
