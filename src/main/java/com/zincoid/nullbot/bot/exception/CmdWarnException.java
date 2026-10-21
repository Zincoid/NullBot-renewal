package com.zincoid.nullbot.bot.exception;

/**
 * 指令流程控制：告警用户并中止指令。
 * <p>Command flow control: warn the user, then abort the command.</p>
 */
public class CmdWarnException extends BotCmdException {

    public CmdWarnException(String message) {
        super(message);
    }

    public CmdWarnException(String message, Throwable cause) {
        super(message, cause);
    }

    public CmdWarnException(Throwable cause) {
        super(cause);
    }
}
