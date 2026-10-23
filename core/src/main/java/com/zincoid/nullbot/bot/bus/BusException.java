package com.zincoid.nullbot.bot.bus;

import com.zincoid.nullbot.platform.exception.BotException;

/**
 * 事件总线异常。
 * <p>Event bus exception.</p>
 */
public class BusException extends BotException {

    public BusException(String message) {
        super(message);
    }

    public BusException(String message, Throwable cause) {
        super(message, cause);
    }
}
