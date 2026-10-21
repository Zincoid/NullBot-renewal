package com.zincoid.nullbot.module.common;

import com.zincoid.nullbot.platform.exception.BotException;

/**
 * 公共服务无法完成请求时抛出。
 * <p>Thrown when a common service cannot complete a request.</p>
 */
public class CommonException extends BotException {

    public CommonException(String message) {
        super(message);
    }

    public CommonException(String message, Throwable cause) {
        super(message, cause);
    }
}
