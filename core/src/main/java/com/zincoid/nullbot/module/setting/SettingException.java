package com.zincoid.nullbot.module.setting;

import com.zincoid.nullbot.platform.exception.NullBotException;

/**
 * 设置服务无法完成请求时抛出。
 * <p>Thrown when a setting service cannot complete a request.</p>
 */
public class SettingException extends NullBotException {

    public SettingException(String message) {
        super(message);
    }

    public SettingException(String message, Throwable cause) {
        super(message, cause);
    }
}
