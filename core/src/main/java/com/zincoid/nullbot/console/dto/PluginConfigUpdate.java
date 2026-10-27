package com.zincoid.nullbot.console.dto;

/**
 * 控制台插件配置保存请求。
 * <p>Console request to save a plugin configuration file.</p>
 *
 * @param content the raw config.properties content
 */
public record PluginConfigUpdate(String content) {
}
