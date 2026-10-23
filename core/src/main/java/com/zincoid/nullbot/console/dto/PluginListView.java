package com.zincoid.nullbot.console.dto;

import java.util.List;

/**
 * 控制台插件列表：全局开关与插件状态。
 * <p>Console plugin list: the global switch and plugin statuses.</p>
 *
 * @param enabled whether plugin loading is enabled
 * @param items   the plugin statuses
 */
public record PluginListView(boolean enabled, List<PluginView> items) {

    public PluginListView {
        items = items == null ? List.of() : List.copyOf(items);
    }
}
