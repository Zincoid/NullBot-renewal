package com.zincoid.nullbot.console.exception;

import com.zincoid.nullbot.platform.exception.BotException;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 控制台接口异常：携带 HTTP 状态与提示信息。
 * <p>Console API exception carrying an HTTP status and a message.</p>
 */
@Getter
public class ConsoleException extends BotException {

    private final HttpStatus status;

    public ConsoleException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public ConsoleException(HttpStatus status, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
    }
}
