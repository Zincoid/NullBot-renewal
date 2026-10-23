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

    private final CmdRegistry registry;

    public Call(CmdRegistry registry) {
        this.registry = registry;
    }

    // ════ Tool ════════════════════

    @Override
    public ToolDefinition definition() {
        Set<String> names = registry.getAiCallableNames();
        String docs = names.stream()
                .map(registry::get)
                .filter(Objects::nonNull)
                .map(Cmd::getAiHelp)
                .collect(Collectors.joining("\n\n"));
        return ToolDefinition.builder("call", "Runs a bot command in the current chat.\n\n" + docs)
                .addEnum("cmd", "command name", names, true)
                .addString("args", "space-separated command arguments")
                .build();
    }

    @Override
    public ToolRes run(ToolContext ctx, String json) {
        Args args = JsonUtils.parse(json, Args.class);
        Cmd cmd = registry.get(args.cmd);
        if (cmd == null || !cmd.isAiCallable())
            return ToolRes.error("Unsupported command: " + args.cmd);
        List<String> params = args.args == null || args.args.isBlank()
                ? List.of()
                : List.of(args.args.trim().split("\\s+"));
        return ctx.cmd(args.cmd, params);
    }

    private record Args(String cmd, String args) {
    }
}
