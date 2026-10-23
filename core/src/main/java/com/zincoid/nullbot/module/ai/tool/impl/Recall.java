package com.zincoid.nullbot.module.ai.tool.impl;

import com.zincoid.nullbot.module.ai.tool.Tool;
import com.zincoid.nullbot.module.ai.tool.ToolContext;
import com.zincoid.nullbot.module.ai.tool.ToolDefinition;
import com.zincoid.nullbot.module.ai.tool.ToolRes;
import com.zincoid.nullbot.platform.util.JsonUtils;
import org.springframework.stereotype.Component;

/**
 * 消息撤回工具：按消息 ID 撤回当前会话中的消息。
 * <p>Message recall tool: recalls a message in the current chat by message id.</p>
 */
@Component
public class Recall implements Tool {

    @Override
    public ToolDefinition definition() {
        return ToolDefinition.builder("recall", """
                        Recalls a message in the current chat by message id.
                        Use the id from the [id=...] prefix in the message history.""")
                .addString("id", "message id", true)
                .build();
    }

    @Override
    public ToolRes run(ToolContext ctx, String args) {
        Args _args = JsonUtils.parse(args, Args.class);
        if (_args.id == null || _args.id.isBlank())
            return ToolRes.error("Missing message id");
        return ctx.recall(_args.id);
    }

    private record Args(String id) {
    }
}
