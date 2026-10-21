package com.zincoid.nullbot.module.ai.model;

import com.zincoid.nullbot.module.ai.message.AiMessage;
import com.zincoid.nullbot.module.ai.tool.ToolDefinition;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

/**
 * 模型请求：消息列表、工具定义、推理强度与输出上限。
 * <p>Model request: messages, tool definitions, reasoning effort, and output limit.</p>
 */
@Getter
@AllArgsConstructor(staticName = "of")
public class ModelReq {

    private final List<AiMessage> messages;
    private final List<ToolDefinition> tools;
    private final Effort effort;
    private final int maxTokens;

    public static ModelReq of(List<AiMessage> messages, Effort effort, int maxTokens) {
        return ModelReq.of(messages, null, effort, maxTokens);
    }
}
