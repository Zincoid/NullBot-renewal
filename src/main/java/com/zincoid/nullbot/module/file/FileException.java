package com.zincoid.nullbot.module.file;

import com.zincoid.nullbot.platform.exception.BotException;

/**
 * Thrown when the file service cannot complete a request.
 * <p>文件服务无法完成请求时抛出。</p>
 */
public class FileException extends BotException {

    public FileException(String message) {
        super(message);
    }

    public FileException(String message, Throwable cause) {
        super(message, cause);
    }
}
