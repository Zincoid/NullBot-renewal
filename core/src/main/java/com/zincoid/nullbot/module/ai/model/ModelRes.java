package com.zincoid.nullbot.module.ai.model;

import com.zincoid.nullbot.module.ai.tool.ToolCall;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

/**
 * 模型响应：工具调用、正文、推理内容与 token 用量。
 * <p>Model response: tool calls, content, reasoning, and token usage.</p>
 */
@Getter
@AllArgsConstructor(staticName = "of")
public class ModelRes {

    private final List<ToolCall> tcs;
    private final String content;
    private final String reasoning;
    private TokenUsage usage;

    public boolean hasTcs() {
        return tcs != null && !tcs.isEmpty();
    }

    public static ModelRes of(String content, String reasoning, TokenUsage usage) {
        return ModelRes.of(List.of(), content, reasoning, usage);
    }

    public static ModelRes of(List<ToolCall> tcs, String reasoning, TokenUsage usage) {
        return ModelRes.of(tcs, null, reasoning, usage);
    }

    public ModelRes withUsage(TokenUsage usage) {
        this.usage = usage;
        return this;
    }
}
