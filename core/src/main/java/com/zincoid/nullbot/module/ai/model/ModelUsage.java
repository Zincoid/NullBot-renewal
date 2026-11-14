package com.zincoid.nullbot.module.ai.model;

/**
 * 模型用量：输入与输出 token 数。
 * <p>Model usage: input and output token counts.</p>
 *
 * @param inputTokens  input token count, nullable when the provider omits it
 * @param outputTokens output token count, nullable when the provider omits it
 */
public record ModelUsage(Integer inputTokens, Integer outputTokens) {

    public static ModelUsage plus(ModelUsage first, ModelUsage second) {
        if (first == null) return second;
        if (second == null) return first;
        return new ModelUsage(
                sum(first.inputTokens, second.inputTokens),
                sum(first.outputTokens, second.outputTokens));
    }

    private static Integer sum(Integer first, Integer second) {
        if (first == null) return second;
        if (second == null) return first;
        return first + second;
    }
}
