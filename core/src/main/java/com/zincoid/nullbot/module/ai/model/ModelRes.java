package com.zincoid.nullbot.module.ai.model;

import com.zincoid.nullbot.module.ai.tool.ToolCall;

import java.util.List;

/**
 * 模型响应：工具调用、正文、推理内容与 token 用量。
 * <p>Model response: tool calls, content, reasoning, and token usage.</p>
 *
 * @param tcs       the tool calls, empty when the response has none
 * @param content   the text content, null when the response only calls tools
 * @param reasoning the reasoning content, nullable
 * @param usage     the token usage, nullable
 */
public record ModelRes(List<ToolCall> tcs, String content, String reasoning, ModelUsage usage) {

    public ModelRes {
        tcs = tcs != null ? tcs : List.of();
    }

    public static ModelRes text(String content, String reasoning, ModelUsage usage) {
        return new ModelRes(List.of(), content, reasoning, usage);
    }

    public static ModelRes tool(List<ToolCall> tcs, String reasoning, ModelUsage usage) {
        return new ModelRes(tcs, null, reasoning, usage);
    }

    public ModelRes withUsage(ModelUsage usage) {
        return new ModelRes(tcs, content, reasoning, usage);
    }
}
