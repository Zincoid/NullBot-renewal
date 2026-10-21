package com.zincoid.nullbot.module.config;

import lombok.Getter;

/**
 * 配置键字典：数据库值优先，条目缺失时回退默认值。
 * <p>Configuration key dictionary: the database value wins; the default value is the fallback when the entry is missing.</p>
 */
@Getter
public enum Conf {

    MESSAGE_HISTORY_MAX_LENGTH("message_history_max_length", "64", "Chat history window length");

    private final String key;
    private final String defaultValue;
    private final String description;

    Conf(String key, String defaultValue, String description) {
        this.key = key;
        this.defaultValue = defaultValue;
        this.description = description;
    }
}
