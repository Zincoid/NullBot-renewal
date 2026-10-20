package com.zincoid.nullbot.bot.exception;

/**
 * Thrown to report a command error.
 * <p>抛出以报告指令错误。</p>
 */
public class CmdErrorException extends BotCmdException {

    public CmdErrorException(String message) {
        super(message);
    }

    public CmdErrorException(String message, Throwable cause) {
        super(message, cause);
    }

    public CmdErrorException(Throwable cause) {
        super(cause);
    }
}
