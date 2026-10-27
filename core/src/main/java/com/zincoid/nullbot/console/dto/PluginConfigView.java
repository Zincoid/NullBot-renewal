package com.zincoid.nullbot.console.dto;

/**
 * 控制台插件配置视图。
 * <p>Console view of a plugin configuration file.</p>
 *
 * @param content the raw config.properties content
 * @param exists  whether the file exists
 */
public record PluginConfigView(String content, boolean exists) {
}
