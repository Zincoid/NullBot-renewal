package com.zincoid.nullbot.console.dto;

import com.zincoid.nullbot.module.config.Conf;
import com.zincoid.nullbot.platform.persistence.po.Config;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 控制台配置项视图。
 * <p>Console view of a configuration entry.</p>
 *
 * @param key          the config key
 * @param value        the effective value
 * @param defaultValue the default value
 * @param description  the description
 * @param secret       whether the entry is marked as secret
 * @param customized   whether the effective value differs from the default
 * @param updatedAt    the last update time, nullable when not stored
 */
public record ConfigView(
        String key,
        String value,
        String defaultValue,
        String description,
        boolean secret,
        boolean customized,
        LocalDateTime updatedAt
) {

    /**
     * 由配置字典与存储记录构建视图。
     * <p>Builds the view from the config dictionary and the stored entry.</p>
     *
     * @param conf   the config dictionary entry
     * @param config the stored entry, nullable
     * @return the view
     */
    public static ConfigView of(Conf conf, Config config) {
        boolean stored = config != null;
        String value = stored && config.getValue() != null ? config.getValue() : conf.getDefaultValue();
        return new ConfigView(
                conf.getKey(),
                value,
                conf.getDefaultValue(),
                conf.getDescription(),
                conf.isSecret(),
                !Objects.equals(value, conf.getDefaultValue()),
                stored ? config.getUpdatedAt() : null);
    }
}
