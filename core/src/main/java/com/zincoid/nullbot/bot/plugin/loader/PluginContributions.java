package com.zincoid.nullbot.bot.plugin.loader;

import com.zincoid.nullbot.bot.cmd.Cmd;
import com.zincoid.nullbot.bot.cmd.CmdRegistry;
import com.zincoid.nullbot.bot.plugin.Plugin;
import com.zincoid.nullbot.bot.plugin.PluginException;
import com.zincoid.nullbot.module.ai.tool.Tool;
import com.zincoid.nullbot.module.ai.tool.ToolRegistry;

import java.util.List;

/**
 * 插件贡献：注册与注销指令和 AI 工具。
 * <p>Plugin contributions: registers and unregisters commands and AI tools.</p>
 *
 * @param cmds  the commands
 * @param tools the AI tools
 */
record PluginContributions(List<Cmd> cmds, List<Tool> tools) {

    PluginContributions {
        cmds = cmds == null ? List.of() : List.copyOf(cmds);
        tools = tools == null ? List.of() : List.copyOf(tools);
    }

    /**
     * 收集插件贡献，并校验工具所需的 API 版本。
     * <p>Collects plugin contributions and validates the API version required by tools.</p>
     *
     * @param plugin     the plugin instance
     * @param apiVersion the plugin API version
     * @return the contributions
     */
    static PluginContributions collect(Plugin plugin, int apiVersion) {
        PluginContributions contributions = new PluginContributions(plugin.cmds(), plugin.tools());
        if (apiVersion < 2 && !contributions.tools().isEmpty())
            throw new PluginException("Plugins providing tools must declare apiVersion 2: " + plugin.id());
        return contributions;
    }

    /**
     * 注册指令与工具；工具注册失败时回滚已注册的指令。
     * <p>Registers commands and tools; rolls back registered commands when tool registration fails.</p>
     *
     * @param cmdRegistry  the command registry
     * @param toolRegistry the AI tool registry
     */
    void register(CmdRegistry cmdRegistry, ToolRegistry toolRegistry) {
        if (!cmdRegistry.register(cmds))
            throw new PluginException("Command name conflict");
        try {
            if (toolRegistry.register(tools)) return;
        } catch (RuntimeException | Error e) {
            cmdRegistry.unregister(cmds);
            throw e;
        }
        cmdRegistry.unregister(cmds);
        throw new PluginException("Tool name conflict");
    }

    /**
     * 注销指令与工具。
     * <p>Unregisters commands and tools.</p>
     *
     * @param cmdRegistry  the command registry
     * @param toolRegistry the AI tool registry
     */
    void unregister(CmdRegistry cmdRegistry, ToolRegistry toolRegistry) {
        cmdRegistry.unregister(cmds);
        toolRegistry.unregister(tools);
    }
}
