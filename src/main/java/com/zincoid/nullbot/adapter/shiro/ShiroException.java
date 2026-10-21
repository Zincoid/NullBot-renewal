package com.zincoid.nullbot.adapter.shiro;

import com.zincoid.nullbot.bot.spi.port.BotSpiException;

/**
 * Shiro 适配器无法完成请求时抛出。
 * <p>Thrown when the Shiro adapter cannot serve the request.</p>
 */
public class ShiroException extends BotSpiException {

    public ShiroException(String message) {
        super(message);
    }

    public ShiroException(String message, Throwable cause) {
        super(message, cause);
    }
}
