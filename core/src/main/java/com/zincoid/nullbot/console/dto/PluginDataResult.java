package com.zincoid.nullbot.console.dto;

import java.util.List;

/**
 * 插件数据批量操作结果。
 * <p>Result of a batch plugin data operation.</p>
 *
 * @param items the per-item results
 */
public record PluginDataResult(List<Item> items) {

    public PluginDataResult {
        items = items == null ? List.of() : List.copyOf(items);
    }

    /**
     * 单项操作结果。
     * <p>A per-item operation result.</p>
     *
     * @param name  the entry name
     * @param ok    whether the operation succeeded
     * @param error the failure message, null on success
     */
    public record Item(String name, boolean ok, String error) {
    }
}
