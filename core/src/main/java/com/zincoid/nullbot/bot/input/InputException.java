package com.zincoid.nullbot.bot.input;

import com.zincoid.nullbot.platform.exception.BotException;

/**
 * 输入等待失败：参数缺失、等待冲突或中断。
 * <p>Input wait failure: missing arguments, wait conflicts, or interruption.</p>
 */
public class InputException extends BotException {

    public InputException(String message) {
        super(message);
    }

    public InputException(String message, Throwable cause) {
        super(message, cause);
    }
}
