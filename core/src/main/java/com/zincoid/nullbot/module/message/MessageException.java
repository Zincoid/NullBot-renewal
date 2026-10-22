package com.zincoid.nullbot.module.message;

import com.zincoid.nullbot.platform.exception.BotException;

/**
 * 消息存储操作失败：参数缺失、内容为空、窗口配置非法或记录不存在。
 * <p>Message store failure: missing arguments, empty content, invalid window configuration, or a missing record.</p>
 */
public class MessageException extends BotException {

    public MessageException(String message) {
        super(message);
    }

    public MessageException(String message, Throwable cause) {
        super(message, cause);
    }
}
