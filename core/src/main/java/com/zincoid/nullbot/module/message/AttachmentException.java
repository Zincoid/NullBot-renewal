package com.zincoid.nullbot.module.message;

import com.zincoid.nullbot.platform.exception.NullBotException;

/**
 * 附件服务无法完成请求时抛出。
 * <p>Thrown when the attachment service cannot complete a request.</p>
 */
public class AttachmentException extends NullBotException {

    public AttachmentException(String message) {
        super(message);
    }

    public AttachmentException(String message, Throwable cause) {
        super(message, cause);
    }
}
