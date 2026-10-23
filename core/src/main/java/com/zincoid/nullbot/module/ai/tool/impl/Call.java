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

    private static final Set<String> CMDS = Set.of(
            "QuoteRand", "QuoteSave",
            "ImageRand", "ImageSave",
            "VideoRand", "VideoSave");

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
        if (args.cmd == null || !CMDS.contains(args.cmd))
            return ToolRes.error("Unsupported command: " + args.cmd);
        List<String> params = args.args == null || args.args.isBlank()
                ? List.of()
                : List.of(args.args.trim().split("\\s+"));
        return ctx.cmd(args.cmd, params);
    }

    // ════ Helpers ═════════════════

    private ToolDefinition build() {
        String docs = CMDS.stream().sorted()
                .map(registry::get)
                .filter(Objects::nonNull)
                .map(Cmd::getAiHelp)
                .collect(Collectors.joining("\n\n"));
        return ToolDefinition.builder("call", "Runs a bot command in the current chat.\n\n" + docs)
                .addEnum("cmd", "command name", CMDS, true)
                .addString("args", "space-separated command arguments")
                .build();
    }

    private record Args(String cmd, String args) {
    }
}
