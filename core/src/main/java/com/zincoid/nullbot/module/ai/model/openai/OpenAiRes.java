package com.zincoid.nullbot.module.ai.model.openai;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.zincoid.nullbot.module.ai.AiException;
import com.zincoid.nullbot.module.ai.model.ModelRes;
import com.zincoid.nullbot.module.ai.model.TokenUsage;
import com.zincoid.nullbot.module.ai.tool.ToolCall;

import java.util.List;
import java.util.Optional;

/**
 * OpenAI 兼容的 Chat Completions 响应体。
 * <p>OpenAI-compatible Chat Completions response body.</p>
 *
 * @param choices the response choices
 * @param usage   the token usage, nullable when omitted by the provider
 */
public record OpenAiRes(List<Choice> choices, Usage usage) {

    public record Choice(Message message) {}

    public record Message(String content,
                          @JsonProperty("reasoning_content") String reasoning,
                          @JsonProperty("tool_calls") List<ToolCallNode> tcs) {}

    public record ToolCallNode(String id, Function function) {}

    public record Function(String name, String arguments) {}

    public record Usage(@JsonProperty("prompt_tokens") Integer promptTokens,
                        @JsonProperty("completion_tokens") Integer completionTokens) {
    }

    /**
     * 转换为模型响应。
     * <p>Converts to the model response.</p>
     *
     * @return the model response
     */
    public ModelRes toModelRes() {
        Message msg = Optional.ofNullable(choices)
                .filter(c -> !c.isEmpty())
                .map(c -> c.getFirst().message)
                .orElse(null);
        if (msg == null)
            throw new AiException("AI no message");
        TokenUsage tokens = usage == null ? null : new TokenUsage(usage.promptTokens(), usage.completionTokens());
        String content = msg.content != null ? msg.content : "";
        String reasoning = msg.reasoning != null ? msg.reasoning : "";
        if (msg.tcs == null || msg.tcs.isEmpty()) {
            if (content.isBlank() && !reasoning.isBlank())
                throw new AiException("AI reasoning truncated");
            return ModelRes.of(content, reasoning, tokens);
        }
        List<ToolCall> calls = msg.tcs.stream()
                .map(tc -> new ToolCall(tc.id, tc.function.name, tc.function.arguments))
                .toList();
        return ModelRes.of(calls, reasoning, tokens);
    }
}
