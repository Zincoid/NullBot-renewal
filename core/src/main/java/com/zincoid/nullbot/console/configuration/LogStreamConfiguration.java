package com.zincoid.nullbot.console.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 日志流配置：启用定时任务以发送 SSE 心跳。
 * <p>Log stream configuration: enables scheduled tasks for SSE heartbeats.</p>
 */
@Configuration
@EnableScheduling
public class LogStreamConfiguration {
}
