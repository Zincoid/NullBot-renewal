package com.zincoid.nullbot.module.render;

import com.zincoid.nullbot.platform.exception.NullBotException;

/**
 * 渲染服务无法完成请求时抛出。
 * <p>Thrown when the render service cannot complete a request.</p>
 */
public class RenderException extends NullBotException {

    public RenderException(String message) {
        super(message);
    }

    public RenderException(String message, Throwable cause) {
        super(message, cause);
    }
}
