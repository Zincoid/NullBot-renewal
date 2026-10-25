package com.zincoid.nullbot.console.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 控制台接口异常处理：处理控制台异常与未捕获异常，标准 MVC 异常交给 Spring Boot。
 * <p>Console API exception handling: handles console and unhandled exceptions,
 * leaving standard MVC exceptions to Spring Boot.</p>
 */
@Slf4j
@RestControllerAdvice(basePackages = "com.zincoid.nullbot.console.controller")
public class ConsoleExceptionHandler {

    /**
     * 返回异常自带的 HTTP 状态与提示，5xx 记录堆栈。
     * <p>Returns the HTTP status and message carried by the exception, logging 5xx with a stack trace.</p>
     *
     * @param e the exception
     * @return the problem detail
     */
    @ExceptionHandler(ConsoleException.class)
    public ProblemDetail handle(ConsoleException e) {
        if (e.getStatus().is5xxServerError())
            log.error("Console request failed: {}", e.getStatus(), e);
        return ProblemDetail.forStatusAndDetail(e.getStatus(), message(e));
    }

    /**
     * 把未捕获异常映射为 500，并返回异常消息。
     * <p>Maps unhandled exceptions to 500 and returns the exception message.</p>
     *
     * @param e the exception
     * @return the problem detail
     */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handle(Exception e) {
        log.error("Console request failed", e);
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, message(e));
    }

    private static String message(Exception e) {
        String message = e.getMessage();
        return message == null || message.isBlank() ? e.getClass().getSimpleName() : message;
    }
}
