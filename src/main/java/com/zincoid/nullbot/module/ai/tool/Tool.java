package com.zincoid.nullbot.module.ai.tool;

/**
 * 模型可调用的工具。
 * <p>A tool callable by the model.</p>
 */
public interface Tool {

    /**
     * 工具定义。
     * <p>Returns the tool definition.</p>
     *
     * @return the tool definition
     */
    ToolDefinition definition();

    /**
     * 执行工具。
     * <p>Runs the tool.</p>
     *
     * @param ctx  the tool context
     * @param args the raw JSON arguments
     * @return the tool result
     */
    ToolRes run(ToolContext ctx, String args);
}
