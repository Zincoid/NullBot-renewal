package com.zincoid.nullbot.bot.spi.port;

import com.zincoid.nullbot.platform.exception.BotException;

/**
 * SPI 异常抽象基类。
 * <p>Abstract base of the SPI exception family.</p>
 */
public abstract class BotSpiException extends BotException {

    protected BotSpiException(String message) {
        super(message);
    }

    protected BotSpiException(String message, Throwable cause) {
        super(message, cause);
    }
}
