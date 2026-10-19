package com.zincoid.nullbot.bot.spi.port;

import com.zincoid.nullbot.platform.exception.BotException;

/**
 * Abstract base of the SPI exception family.
 * <p>SPI 异常抽象基类。</p>
 */
public abstract class BotSpiException extends BotException {

    protected BotSpiException(String message) {
        super(message);
    }

    protected BotSpiException(String message, Throwable cause) {
        super(message, cause);
    }
}
