package com.zincoid.nullbot.bot;

import com.zincoid.nullbot.platform.exception.NullBotException;

/**
 * 机器人账号状态异常：重复上线或离线时访问账号。
 * <p>Bot account state exception: a duplicate online or an access while offline.</p>
 */
public class BotException extends NullBotException {

    public BotException(String message) {
        super(message);
    }

    public BotException(String message, Throwable cause) {
        super(message, cause);
    }
}
