package com.zincoid.nullbot.console.dto;

/**
 * 新建插件数据文件夹请求。
 * <p>Request to create a plugin data folder.</p>
 *
 * @param path the parent path relative to the plugin data root, empty for the root
 * @param name the new folder name
 */
public record PluginDataFolder(String path, String name) {
}
