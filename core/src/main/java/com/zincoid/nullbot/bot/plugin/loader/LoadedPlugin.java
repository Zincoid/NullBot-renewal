package com.zincoid.nullbot.bot.plugin.loader;

import com.zincoid.nullbot.bot.cmd.Cmd;
import com.zincoid.nullbot.bot.cmd.CmdMapping;
import com.zincoid.nullbot.bot.plugin.Plugin;
import com.zincoid.nullbot.module.ai.tool.Tool;

import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

/**
 * 已加载插件：元数据、源 JAR 指纹、私有副本、类加载器、插件实例、注册指令与 AI 工具。
 * <p>Loaded plugin: metadata, source jar stamp, private copy, classloader, plugin instance,
 * registered commands, and AI tools.</p>
 *
 * @param id          the plugin id
 * @param version     the plugin version
 * @param apiVersion  the plugin API version
 * @param jarName     the plugin jar filename
 * @param jarCopy     the private jar copy used by the classloader
 * @param jarModified the source jar last-modified time
 * @param jarSize     the source jar size in bytes
 * @param loader      the plugin classloader
 * @param plugin      the plugin instance
 * @param cmds        the registered command instances
 * @param tools       the registered AI tool instances
 */
record LoadedPlugin(
        String id,
        String version,
        int apiVersion,
        String jarName,
        Path jarCopy,
        FileTime jarModified,
        long jarSize,
        URLClassLoader loader,
        Plugin plugin,
        List<Cmd> cmds,
        List<Tool> tools
) {

    LoadedPlugin {
        cmds = cmds == null ? List.of() : List.copyOf(cmds);
        tools = tools == null ? List.of() : List.copyOf(tools);
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
