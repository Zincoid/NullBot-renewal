package com.zincoid.nullbot.bot.memory;

import com.zincoid.nullbot.platform.exception.BotException;

/**
 * 消息窗口操作失败：参数缺失或窗口配置非法。
 * <p>Message window failure: missing arguments or an invalid window configuration.</p>
 */
public class MemoryException extends BotException {

    public MemoryException(String message) {
        super(message);
    }

    public MemoryException(String message, Throwable cause) {
        super(message, cause);
    }
}
