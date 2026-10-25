package com.zincoid.nullbot.bot.plugin.loader;

import java.util.List;

/**
 * 插件状态：JAR、元数据、指令与加载结果。
 * <p>Plugin status: jar, metadata, commands, and load result.</p>
 *
 * @param jar        the plugin jar filename
 * @param id         the plugin id from the loaded instance or persisted mapping, null when unknown
 * @param version    the plugin version, null when not loaded
 * @param apiVersion the plugin API version, null when not loaded
 * @param commands   the registered command aliases
 * @param state      the load state
 * @param error      the load failure message, null unless failed
 */
public record PluginStatus(
        String jar,
        String id,
        String version,
        Integer apiVersion,
        List<String> commands,
        State state,
        String error
) {

    public PluginStatus {
        commands = commands == null ? List.of() : List.copyOf(commands);
    }

    /**
     * 插件加载状态。
     * <p>Plugin load state.</p>
     */
    public enum State {

        /**
         * 已加载并注册指令与工具。
         * <p>Loaded with commands and tools registered.</p>
         */
        LOADED,

        /**
         * 存在于插件目录但已被禁用。
         * <p>Present in the plugin directory but disabled.</p>
         */
        DISABLED,

        /**
         * 未被禁用，但最近一次加载失败。
         * <p>Not disabled, but the last load failed.</p>
         */
        FAILED,

        /**
         * 存在于插件目录且未被禁用，但当前未加载。
         * <p>Present and not disabled, but currently not loaded.</p>
         */
        AVAILABLE
    }
}
