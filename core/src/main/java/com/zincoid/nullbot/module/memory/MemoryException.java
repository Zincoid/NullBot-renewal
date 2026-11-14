package com.zincoid.nullbot.module.memory;

import com.zincoid.nullbot.platform.exception.NullBotException;

/**
 * 记忆存储操作失败：参数缺失、内容非法或记录不存在。
 * <p>Memory store failure: missing arguments, invalid content, or a missing record.</p>
 */
public class MemoryException extends NullBotException {

    public MemoryException(String message) {
        super(message);
    }

    public MemoryException(String message, Throwable cause) {
        super(message, cause);
    }
}
