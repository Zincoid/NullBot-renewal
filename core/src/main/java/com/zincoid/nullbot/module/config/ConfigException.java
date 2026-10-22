package com.zincoid.nullbot.module.config;

import com.zincoid.nullbot.platform.exception.BotException;

/**
 * 配置服务无法完成请求时抛出。
 * <p>Thrown when the configuration service cannot complete a request.</p>
 */
public class ConfigException extends BotException {
    public ConfigException(String message) { super(message); }
    public ConfigException(String message, Throwable cause) { super(message, cause); }
}
