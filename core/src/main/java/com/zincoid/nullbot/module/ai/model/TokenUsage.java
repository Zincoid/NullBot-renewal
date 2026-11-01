package com.zincoid.nullbot.module.ai.model;

/**
 * Token 用量：输入与输出 token 数。
 * <p>Token usage: input and output token counts.</p>
 *
 * @param inputTokens  input token count, nullable when the provider omits it
 * @param outputTokens output token count, nullable when the provider omits it
 */
public record TokenUsage(Integer inputTokens, Integer outputTokens) {

    /**
     * 合并两次用量，空值按缺失处理。
     * <p>Combines two usage values, treating null as missing.</p>
     *
     * @param first  the first usage, nullable
     * @param second the second usage, nullable
     * @return the combined usage, or null when both are null
     */
    public static TokenUsage plus(TokenUsage first, TokenUsage second) {
        if (first == null) return second;
        if (second == null) return first;
        return new TokenUsage(
                sum(first.inputTokens, second.inputTokens),
                sum(first.outputTokens, second.outputTokens));
    }

    private static Integer sum(Integer first, Integer second) {
        if (first == null) return second;
        if (second == null) return first;
        return first + second;
    }
}
