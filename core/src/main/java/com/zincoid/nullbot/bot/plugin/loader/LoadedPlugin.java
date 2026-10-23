package com.zincoid.nullbot.bot.plugin.loader;

import com.zincoid.nullbot.bot.cmd.Cmd;
import com.zincoid.nullbot.bot.cmd.CmdMapping;
import com.zincoid.nullbot.bot.plugin.Plugin;

import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

/**
 * 已加载插件：元数据、类加载器、插件实例与注册指令。
 * <p>Loaded plugin: metadata, classloader, plugin instance, and registered commands.</p>
 *
 * @param id         the plugin id
 * @param version    the plugin version
 * @param apiVersion the plugin API version
 * @param jarName    the plugin jar filename
 * @param loader     the plugin classloader
 * @param plugin     the plugin instance
 * @param cmds       the registered command instances
 */
record LoadedPlugin(
        String id,
        String version,
        int apiVersion,
        String jarName,
        URLClassLoader loader,
        Plugin plugin,
        List<Cmd> cmds
) {

    LoadedPlugin {
        cmds = cmds == null ? List.of() : List.copyOf(cmds);
    }

    /**
     * 构建插件状态视图。
     * <p>Builds the plugin status view.</p>
     *
     * @return the plugin status
     */
    PluginStatus status() {
        List<String> commands = cmds.stream()
                .flatMap(cmd -> {
                    CmdMapping mapping = cmd.getClass().getAnnotation(CmdMapping.class);
                    return mapping == null ? Stream.empty() : Arrays.stream(mapping.value());
                })
                .distinct()
                .toList();
        return new PluginStatus(jarName, id, version, apiVersion,
                commands, PluginStatus.State.LOADED, null);
    }
}
