package com.zincoid.nullbot.module.ai.tool.impl;

import com.zincoid.nullbot.module.ai.tool.Tool;
import com.zincoid.nullbot.module.ai.tool.ToolContext;
import com.zincoid.nullbot.module.ai.tool.ToolDefinition;
import com.zincoid.nullbot.module.ai.tool.ToolRes;
import com.zincoid.nullbot.platform.util.JsonUtils;
import org.springframework.stereotype.Component;

/**
 * 禁言工具：对当前群成员执行禁言或解除禁言。
 * <p>Mute tool: mutes or unmutes a member of the current group.</p>
 */
@Component
public class Mute implements Tool {

    @Override
    public ToolDefinition definition() {
        return ToolDefinition.builder("mute", """
                        Mutes a member of the current group chat.
                        Use 0 minutes to unmute.""")
                .addString("user", "target user id", true)
                .addInteger("minutes", "duration in minutes, 0 to unmute", true)
                .build();
    }

    @Override
    public ToolRes run(ToolContext ctx, String args) {
        Args _args = JsonUtils.parse(args, Args.class);
        if (_args.user == null || _args.user.isBlank())
            return ToolRes.error("Missing user");
        if (_args.minutes == null)
            return ToolRes.error("Missing minutes");
        return ctx.mute(_args.user, _args.minutes);
    }

    private record Args(String user, Integer minutes) {
    }
}
