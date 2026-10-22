package com.zincoid.nullbot.bot.plugin;

import com.zincoid.nullbot.platform.exception.BotException;

/**
 * 插件加载、校验或生命周期失败。
 * <p>Plugin loading, validation, or lifecycle failure.</p>
 */
public class PluginException extends BotException {

    public PluginException(String message) {
        super(message);
    }

    public PluginException(String message, Throwable cause) {
        super(message, cause);
    }
}
