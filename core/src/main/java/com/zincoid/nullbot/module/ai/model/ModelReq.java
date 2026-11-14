package com.zincoid.nullbot.module.ai.model;

import com.zincoid.nullbot.module.ai.message.AiMsg;
import com.zincoid.nullbot.module.ai.tool.ToolDefinition;
import com.zincoid.nullbot.platform.persistence.enums.Effort;

import java.util.List;

/**
 * 模型请求：消息列表、工具定义、推理强度与输出上限。
 * <p>Model request: messages, tool definitions, reasoning effort, and output limit.</p>
 *
 * @param messages  the messages of the request
 * @param tools     the tool definitions, empty when no tools are provided
 * @param effort    the reasoning effort, nullable
 * @param maxTokens the output token limit
 */
public record ModelReq(List<AiMsg> messages, List<ToolDefinition> tools, Effort effort, int maxTokens) {

    public ModelReq {
        tools = tools != null ? tools : List.of();
    }

    public static ModelReq of(List<AiMsg> messages, List<ToolDefinition> tools, Effort effort, int maxTokens) {
        return new ModelReq(messages, tools, effort, maxTokens);
    }

    public static ModelReq of(List<AiMsg> messages, Effort effort, int maxTokens) {
        return new ModelReq(messages, null, effort, maxTokens);
    }
}
