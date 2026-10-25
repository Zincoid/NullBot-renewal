package com.zincoid.nullbot.console.log;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import com.zincoid.nullbot.console.service.LogStreamService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 桥接 Logback 与控制台日志流服务的根日志追加器。
 * <p>Logback root appender bridging logging events into the console log stream service.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LogStreamAppender extends AppenderBase<ILoggingEvent> {

    private final LogStreamService logStreamService;

    /**
     * 挂载到根日志记录器。
     * <p>Attaches to the root logger.</p>
     */
    @PostConstruct
    public void attach() {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        setContext(context);
        setName("console-log-stream");
        start();
        context.getLogger(Logger.ROOT_LOGGER_NAME).addAppender(this);
        log.info("Log stream appender attached");
    }

    /**
     * 从根日志记录器卸载。
     * <p>Detaches from the root logger.</p>
     */
    @PreDestroy
    public void detach() {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        context.getLogger(Logger.ROOT_LOGGER_NAME).detachAppender(this);
        stop();
    }

    @Override
    protected void append(ILoggingEvent event) {
        logStreamService.broadcast(event);
    }
}
