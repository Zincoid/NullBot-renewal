package com.zincoid.nullbot.bot.cmd;

/**
 * 指令流程控制：提示用户并中止指令。
 * <p>Command flow control: inform the user, then abort the command.</p>
 */
public class CmdInfoException extends CmdException {

    public CmdInfoException(String message) {
        super(message);
    }

    public CmdInfoException(String message, Throwable cause) {
        super(message, cause);
    }

    public CmdInfoException(Throwable cause) {
        super(cause);
    }
}
