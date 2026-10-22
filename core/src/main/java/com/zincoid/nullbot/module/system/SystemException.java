package com.zincoid.nullbot.module.system;

import com.zincoid.nullbot.platform.exception.BotException;

/**
 * 系统服务无法完成请求时抛出。
 * <p>Thrown when a system service cannot complete a request.</p>
 */
public class SystemException extends BotException {

    public SystemException(String message) {
        super(message);
    }

    public SystemException(String message, Throwable cause) {
        super(message, cause);
    }
}
