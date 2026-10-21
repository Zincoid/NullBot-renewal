package com.zincoid.nullbot.module.ai.tool.impl;

import com.zincoid.nullbot.module.ai.tool.Tool;
import com.zincoid.nullbot.module.ai.tool.ToolContext;
import com.zincoid.nullbot.module.ai.tool.ToolDefinition;
import com.zincoid.nullbot.module.ai.tool.ToolRes;
import com.zincoid.nullbot.platform.util.JsonUtils;
import org.springframework.stereotype.Component;

/**
 * 中间回复工具：在最终回复前向当前会话发送一条消息。
 * <p>Intermediate reply tool: sends a message to the current chat before the final reply.</p>
 */
@Component
public class Send implements Tool {

    @Override
    public ToolDefinition definition() {
        return ToolDefinition.builder("send", """
                        Sends an intermediate message to the current chat before the final reply.""")
                .addString("content", "message text", true)
                .addString("reply", "message id to quote, optional")
                .build();
    }

    @Override
    public ToolRes run(ToolContext ctx, String args) {
        Args _args = JsonUtils.parse(args, Args.class);
        if (_args.content == null || _args.content.isBlank())
            return ToolRes.error("Missing content");
        return ToolRes.of("Message sent: " + ctx.send(_args.content, _args.reply));
    }

    private record Args(String content, String reply) {
    }
}
