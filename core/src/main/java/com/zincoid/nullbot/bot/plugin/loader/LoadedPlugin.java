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
 * 已加载插件：元数据、JAR 信息、类加载器、插件实例与注册贡献。
 * <p>Loaded plugin: metadata, jar info, classloader, plugin instance, and registered contributions.</p>
 *
 * @param id            the plugin id
 * @param version       the plugin version
 * @param apiVersion    the plugin API version
 * @param jar           the source and private jar info
 * @param loader        the plugin classloader
 * @param plugin        the plugin instance
 * @param contributions the registered commands and AI tools
 */
record LoadedPlugin(
        String id,
        String version,
        int apiVersion,
        PluginJar jar,
        URLClassLoader loader,
        Plugin plugin,
        PluginContributions contributions
) {

    LoadedPlugin {
        contributions = contributions == null
                ? new PluginContributions(List.of(), List.of())
                : contributions;
    }

    String jarName() {
        return jar.name();
    }

    Path jarCopy() {
        return jar.copy();
    }

    FileTime jarModified() {
        return jar.modified();
    }

    long jarSize() {
        return jar.size();
    }

    List<Cmd> cmds() {
        return contributions.cmds();
    }

    List<Tool> tools() {
        return contributions.tools();
    }

    /**
     * 构建插件状态视图。
     * <p>Builds the plugin status view.</p>
     *
     * @return the plugin status
     */
    PluginStatus status() {
        List<String> cmds = cmds().stream()
                .flatMap(cmd -> {
                    CmdMapping mapping = cmd.getClass().getAnnotation(CmdMapping.class);
                    return mapping == null ? Stream.empty() : Arrays.stream(mapping.value());
                })
                .distinct()
                .toList();
        List<String> tools = tools().stream()
                .map(tool -> tool.definition().getName())
                .toList();
        return PluginStatus.loaded(jarName(), id, version, apiVersion, cmds, tools);
    }
}
