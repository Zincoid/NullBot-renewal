package com.zincoid.nullbot.module.ai.tool.impl;

import com.zincoid.nullbot.bot.cmd.Cmd;
import com.zincoid.nullbot.bot.cmd.CmdRegistry;
import com.zincoid.nullbot.module.ai.tool.Tool;
import com.zincoid.nullbot.module.ai.tool.ToolContext;
import com.zincoid.nullbot.module.ai.tool.ToolDefinition;
import com.zincoid.nullbot.module.ai.tool.ToolRes;
import com.zincoid.nullbot.platform.util.JsonUtils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 指令工具：让模型调用受支持的机器人指令。
 * <p>Command tool: lets the model invoke supported bot commands.</p>
 */
@Component
public class Call implements Tool {

    private static final Set<String> COMMANDS = Set.of(
            "QuoteRand", "QuoteGet", "QuoteSave",
            "ImageRand", "ImageGet", "ImageSave",
            "VideoRand", "VideoGet", "VideoSave");

    private final CmdRegistry registry;
    private final ToolDefinition definition;

    public Call(CmdRegistry registry) {
        this.registry = registry;
        this.definition = build();
    }

    // ════ Tool ════════════════════

    @Override
    public ToolDefinition definition() {
        return definition;
    }

    @Override
    public ToolRes run(ToolContext ctx, String json) {
        Args args = JsonUtils.parse(json, Args.class);
        if (args.command == null || !COMMANDS.contains(args.command))
            return ToolRes.error("Unsupported command: " + args.command);
        List<String> params = args.args == null || args.args.isBlank()
                ? List.of()
                : List.of(args.args.trim().split("\\s+"));
        return ctx.command(args.command, params);
    }

    // ════ Helpers ═════════════════

    private ToolDefinition build() {
        String docs = COMMANDS.stream().sorted()
                .map(registry::get)
                .filter(Objects::nonNull)
                .map(Cmd::getAiHelp)
                .collect(Collectors.joining("\n\n"));
        return ToolDefinition.builder("call", "Runs a bot command in the current chat.\n\n" + docs)
                .addEnum("command", "command name", COMMANDS, true)
                .addString("args", "space-separated command arguments")
                .build();
    }

    private record Args(String command, String args) {
    }
}
