package com.zincoid.nullbot.bot.exception;

import com.zincoid.nullbot.platform.exception.BotException;

/**
 * Abstract base of the command exception family.
 * <p>指令异常抽象基类。</p>
 */
public abstract class BotCmdException extends BotException {

    protected BotCmdException(String message) {
        super(message);
    }

    protected BotCmdException(String message, Throwable cause) {
        super(message, cause);
    }

    protected BotCmdException(Throwable cause) {
        super(cause.toString(), cause);
    }
}
