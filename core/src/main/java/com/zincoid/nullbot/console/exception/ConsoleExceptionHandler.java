package com.zincoid.nullbot.console.exception;

import com.zincoid.nullbot.platform.exception.BotException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * 控制台接口通用异常处理。
 * <p>Generic exception handling of the console API.</p>
 */
@Slf4j
@RestControllerAdvice(basePackages = "com.zincoid.nullbot.console.controller")
public class ConsoleExceptionHandler {

    /**
     * 返回异常自带的 HTTP 状态与提示。
     * <p>Returns the HTTP status and message carried by the exception.</p>
     *
     * @param e the exception
     * @return the problem detail
     */
    @ExceptionHandler(ConsoleException.class)
    public ProblemDetail handle(ConsoleException e) {
        return ProblemDetail.forStatusAndDetail(e.getStatus(), e.getMessage());
    }

    /**
     * 汇总请求体字段校验错误并返回 400。
     * <p>Collects request-body field validation errors and returns 400.</p>
     *
     * @param e the validation exception
     * @return the problem detail
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handle(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(error -> "Invalid " + error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        if (message.isEmpty()) message = "Validation failed";
        log.warn("Console validation failed: {}", message);
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, message);
    }

    /**
     * 把服务层异常映射为 500。
     * <p>Maps service-layer failures to 500.</p>
     *
     * @param e the exception
     * @return the problem detail
     */
    @ExceptionHandler(BotException.class)
    public ProblemDetail handle(BotException e) {
        log.warn("Console request failed: {}", e.getMessage());
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
    }

    /**
     * 把未预期异常映射为 500，并记录堆栈。
     * <p>Maps unexpected failures to 500 and logs the stack trace.</p>
     *
     * @param e the exception
     * @return the problem detail
     */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handle(Exception e) {
        log.error("Console request failed", e);
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");
    }
}
