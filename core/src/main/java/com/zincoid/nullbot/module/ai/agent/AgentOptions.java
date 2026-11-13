package com.zincoid.nullbot.module.ai.agent;

import com.zincoid.nullbot.module.ai.AiException;
import com.zincoid.nullbot.platform.persistence.enums.Effort;

/**
 * 单次对话的调用参数：推理强度、输出上限与工具轮数上限。
 * <p>Per-conversation invocation options: reasoning effort, output limit, and tool round limit.</p>
 *
 * @param effort       reasoning effort
 * @param maxTokens    maximum output tokens
 * @param maxToolCalls maximum tool call rounds
 */
public record AgentOptions(Effort effort, int maxTokens, int maxToolCalls) {

    public AgentOptions {
        if (effort == null) throw new AiException("Missing key: effort");
        if (maxTokens <= 0) throw new AiException("Malformed value: " + maxTokens);
        if (maxToolCalls < 0) throw new AiException("Malformed value: " + maxToolCalls);
    }
}
