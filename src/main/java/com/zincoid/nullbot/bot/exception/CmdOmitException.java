package com.zincoid.nullbot.bot.exception;

/**
 * 指令流程控制：静默中止指令。
 * <p>Command flow control: abort the command silently.</p>
 */
public class CmdOmitException extends BotCmdException {

    public CmdOmitException(String message) {
        super(message);
    }

    public CmdOmitException(String message, Throwable cause) {
        super(message, cause);
    }

    public CmdOmitException(Throwable cause) {
        super(cause);
    }
}
