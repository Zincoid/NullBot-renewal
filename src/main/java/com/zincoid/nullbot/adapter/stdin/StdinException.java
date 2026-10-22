package com.zincoid.nullbot.adapter.stdin;

import com.zincoid.nullbot.bot.spi.port.SpiException;

/**
 * Stdin 适配器无法完成请求时抛出。
 * <p>Thrown when the Stdin adapter cannot serve the request.</p>
 */
public class StdinException extends SpiException {

    public StdinException(String message) {
        super(message);
    }

    public StdinException(String message, Throwable cause) {
        super(message, cause);
    }
}
