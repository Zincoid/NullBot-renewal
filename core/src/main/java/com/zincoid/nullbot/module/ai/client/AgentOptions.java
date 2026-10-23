package com.zincoid.nullbot.module.ai.client;

import com.zincoid.nullbot.module.ai.AiException;

/**
 * 单次对话的调用参数：输出上限与工具轮数上限。
 * <p>Per-conversation invocation options: output limit and tool round limit.</p>
 *
 * @param maxTokens     maximum output tokens
 * @param maxToolRounds maximum tool call rounds
 */
public record AgentOptions(int maxTokens, int maxToolRounds) {

    public AgentOptions {
        if (maxTokens <= 0) throw new AiException("Malformed value: " + maxTokens);
        if (maxToolRounds < 0) throw new AiException("Malformed value: " + maxToolRounds);
    }
}
