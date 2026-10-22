package com.zincoid.nullbot.platform.exception;

/**
 * 平台层无法完成请求时抛出。
 * <p>Thrown when a platform-layer operation cannot complete.</p>
 */
public class PlatformException extends BotException {

    public PlatformException(String message) {
        super(message);
    }

    public PlatformException(String message, Throwable cause) {
        super(message, cause);
    }
}
