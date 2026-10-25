package com.zincoid.nullbot.console.dto;

import java.util.List;

/**
 * 控制台插件数据目录视图。
 * <p>Console view of a plugin data directory.</p>
 *
 * @param path    the path relative to the plugin data root, empty for the root
 * @param entries the directory entries
 */
public record PluginDataView(String path, List<Entry> entries) {

    public PluginDataView {
        entries = entries == null ? List.of() : List.copyOf(entries);
    }

    /**
     * 数据目录条目。
     * <p>A plugin data directory entry.</p>
     *
     * @param name     the entry name
     * @param type     the entry type, {@code dir} or {@code file}
     * @param size     the file size in bytes, zero for directories
     * @param modified the last modified time
     */
    public record Entry(String name, String type, long size, String modified) {
    }
}
