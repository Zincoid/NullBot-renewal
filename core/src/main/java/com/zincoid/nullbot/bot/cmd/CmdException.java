package com.zincoid.nullbot.bot.cmd;

import com.zincoid.nullbot.platform.exception.NullBotException;

/**
 * 指令异常基类。
 * <p>Base of the command exception family.</p>
 */
public class CmdException extends NullBotException {

    public CmdException(String message) {
        super(message);
    }

    public CmdException(String message, Throwable cause) {
        super(message, cause);
    }

    public CmdException(Throwable cause) {
        super(cause.toString(), cause);
    }
}
