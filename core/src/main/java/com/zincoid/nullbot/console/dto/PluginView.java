package com.zincoid.nullbot.console.dto;

import com.zincoid.nullbot.bot.plugin.loader.PluginStatus;

import java.util.List;

/**
 * 控制台插件视图。
 * <p>Console view of a plugin.</p>
 *
 * @param jar        the plugin jar filename
 * @param id         the plugin id, null when not loaded
 * @param version    the plugin version, null when not loaded
 * @param apiVersion the plugin API version, null when not loaded
 * @param commands   the registered command aliases
 * @param state      the load state
 * @param error      the load failure message, null unless failed
 */
public record PluginView(
        String jar,
        String id,
        String version,
        Integer apiVersion,
        List<String> commands,
        String state,
        String error
) {

    public PluginView {
        commands = commands == null ? List.of() : List.copyOf(commands);
    }

    /**
     * 从插件状态构建控制台视图。
     * <p>Builds the console view from a plugin status.</p>
     *
     * @param status the plugin status
     * @return the console view
     */
    public static PluginView of(PluginStatus status) {
        return new PluginView(status.jar(), status.id(), status.version(), status.apiVersion(),
                status.commands(), status.state().name(), status.error());
    }
}
