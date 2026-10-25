package com.zincoid.nullbot.bot.plugin.loader;

import java.util.List;

/**
 * 插件状态：JAR、元数据、指令、工具与加载结果。
 * <p>Plugin status: jar, metadata, commands, tools, and load result.</p>
 *
 * @param jar        the plugin jar filename
 * @param id         the plugin id from the loaded instance or persisted mapping, null when unknown
 * @param version    the plugin version, null when not loaded
 * @param apiVersion the plugin API version, null when not loaded
 * @param cmds       the registered command aliases
 * @param tools      the registered AI tool names
 * @param state      the load state
 * @param error      the load failure message, null unless failed
 */
public record PluginStatus(
        String jar,
        String id,
        String version,
        Integer apiVersion,
        List<String> cmds,
        List<String> tools,
        State state,
        String error
) {

    public PluginStatus {
        cmds = cmds == null ? List.of() : List.copyOf(cmds);
        tools = tools == null ? List.of() : List.copyOf(tools);
    }

    /**
     * 构建已加载状态。
     * <p>Builds the loaded state.</p>
     *
     * @param jar        the plugin jar filename
     * @param id         the plugin id
     * @param version    the plugin version
     * @param apiVersion the plugin API version
     * @param cmds       the registered command aliases
     * @param tools      the registered AI tool names
     * @return the plugin status
     */
    static PluginStatus loaded(String jar, String id, String version, Integer apiVersion,
                               List<String> cmds, List<String> tools) {
        return new PluginStatus(jar, id, version, apiVersion, cmds, tools, State.LOADED, null);
    }

    /**
     * 构建已禁用状态。
     * <p>Builds the disabled state.</p>
     *
     * @param jar the plugin jar filename
     * @param id  the plugin id, nullable
     * @return the plugin status
     */
    static PluginStatus disabled(String jar, String id) {
        return new PluginStatus(jar, id, null, null, List.of(), List.of(), State.DISABLED, null);
    }

    /**
     * 构建可用但未加载状态。
     * <p>Builds the available but unloaded state.</p>
     *
     * @param jar the plugin jar filename
     * @param id  the plugin id, nullable
     * @return the plugin status
     */
    static PluginStatus available(String jar, String id) {
        return new PluginStatus(jar, id, null, null, List.of(), List.of(), State.AVAILABLE, null);
    }

    /**
     * 构建加载失败状态。
     * <p>Builds the failed state.</p>
     *
     * @param jar   the plugin jar filename
     * @param id    the plugin id, nullable
     * @param error the load failure message
     * @return the plugin status
     */
    static PluginStatus failed(String jar, String id, String error) {
        return new PluginStatus(jar, id, null, null, List.of(), List.of(), State.FAILED, error);
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
