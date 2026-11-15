package com.zincoid.nullbot.module.ai.agent;

import com.zincoid.nullbot.module.ai.AiException;
import com.zincoid.nullbot.platform.persistence.enums.Effort;

/**
 * 单次对话的调用参数：推理强度、输出上限、工具轮数上限与记忆开关。
 * <p>Per-conversation invocation options: reasoning effort, output limit, tool round limit, and memory switches.</p>
 *
 * @param effort                reasoning effort
 * @param maxTokens             maximum output tokens
 * @param maxToolCalls          maximum tool call rounds
 * @param episodicMemoryEnabled whether the episodic summary is injected
 * @param semanticMemoryEnabled whether semantic facts are injected and the memory tool is available
 */
public record ChatOptions(Effort effort, int maxTokens, int maxToolCalls,
                          boolean episodicMemoryEnabled, boolean semanticMemoryEnabled) {

    public ChatOptions {
        if (effort == null) throw new AiException("Missing key: effort");
        if (maxTokens <= 0) throw new AiException("Malformed value: " + maxTokens);
        if (maxToolCalls < 0) throw new AiException("Malformed value: " + maxToolCalls);
    }
}
