package com.zincoid.nullbot.console.dto;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * 控制台实时日志视图。
 * <p>Console view of a real-time log entry.</p>
 *
 * @param timestamp  the formatted event time
 * @param level      the log level
 * @param logger     the logger name
 * @param thread     the emitting thread
 * @param message    the formatted message
 * @param stackTrace the throwable stack, null when absent
 */
public record LogView(
        String timestamp,
        String level,
        String logger,
        String thread,
        String message,
        String stackTrace
) {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    /**
     * 从日志事件构建视图，异常时附带完整堆栈。
     * <p>Builds a view from a logging event, including the full stack when a throwable is present.</p>
     *
     * @param event the logging event
     * @return the log entry view
     */
    public static LogView of(ILoggingEvent event) {
        String stackTrace = event.getThrowableProxy() == null
                ? null
                : ThrowableProxyUtil.asString(event.getThrowableProxy());
        return new LogView(
                FORMATTER.format(Instant.ofEpochMilli(event.getTimeStamp()).atZone(ZoneId.systemDefault())),
                event.getLevel().toString(),
                event.getLoggerName(),
                event.getThreadName(),
                event.getFormattedMessage(),
                stackTrace);
    }
}
