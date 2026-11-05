package com.zincoid.nullbot.module.ai;

import com.zincoid.nullbot.platform.exception.NullBotException;

/**
 * AI 服务无法完成请求时抛出。
 * <p>Thrown when an AI service cannot complete a request.</p>
 */
public class AiException extends NullBotException {

    public AiException(String message) {
        super(message);
    }

    public AiException(String message, Throwable cause) {
        super(message, cause);
    }
}
