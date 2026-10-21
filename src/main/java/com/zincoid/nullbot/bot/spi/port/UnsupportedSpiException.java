package com.zincoid.nullbot.bot.spi.port;

/**
 * 框架适配器不支持某出站能力时抛出。
 * <p>Thrown when the framework adapter does not support an outbound capability.</p>
 */
public class UnsupportedSpiException extends BotSpiException {

    public UnsupportedSpiException(String operation) {
        super("Operation not supported: " + operation);
    }
}
