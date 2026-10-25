package com.zincoid.nullbot.console.dto;

import java.util.List;

/**
 * 批量删除插件数据条目请求。
 * <p>Request to delete plugin data entries in batch.</p>
 *
 * @param path  the parent path relative to the plugin data root, empty for the root
 * @param names the entry names to delete
 */
public record PluginDataDelete(String path, List<String> names) {

    public PluginDataDelete {
        names = names == null ? List.of() : List.copyOf(names);
    }
}
