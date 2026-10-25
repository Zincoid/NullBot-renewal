package com.zincoid.hello;

import com.zincoid.nullbot.module.ai.tool.Tool;
import com.zincoid.nullbot.module.ai.tool.ToolContext;
import com.zincoid.nullbot.module.ai.tool.ToolDefinition;
import com.zincoid.nullbot.module.ai.tool.ToolRes;
import com.zincoid.nullbot.platform.util.JsonUtils;

/**
 * 示例工具：通过 AI 工具调用返回问候。
 * <p>Sample tool: returns a greeting through an AI tool call.</p>
 */
public class HelloTool implements Tool {

    @Override
    public ToolDefinition definition() {
        return ToolDefinition.builder("hello_echo", "Returns a greeting from the hello plugin.")
                .addString("text", "text to include in the greeting")
                .build();
    }

    @Override
    public ToolRes run(ToolContext ctx, String args) {
        Args _args = args == null || args.isBlank()
                ? new Args(null)
                : JsonUtils.parse(args, Args.class);
        String text = _args.text == null || _args.text.isBlank()
                ? "Hello from the hello plugin."
                : "Hello, " + _args.text + "!";
        return ToolRes.of(text);
    }

    private record Args(String text) {
    }
}
