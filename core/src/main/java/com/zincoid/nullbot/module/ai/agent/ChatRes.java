package com.zincoid.nullbot.module.ai.agent;

import com.zincoid.nullbot.module.ai.AiException;
import com.zincoid.nullbot.module.ai.model.ModelRes;
import com.zincoid.nullbot.module.ai.model.ModelUsage;

/**
 * 单次对话的结果：最终正文、推理内容、token 用量与本轮是否已发出最终消息。
 * <p>Result of one conversation: the final content, reasoning, token usage, and whether the final message was already sent in this turn.</p>
 *
 * @param content   the final content, null when the final message was already sent
 * @param reasoning the reasoning content, nullable
 * @param usage     the token usage, nullable
 * @param finalSent whether the final message was already sent
 */
public record ChatRes(String content, String reasoning, ModelUsage usage, boolean finalSent) {

    public ChatRes {
        if (!finalSent && (content == null || content.isBlank()))
            throw new AiException("Empty AI response");
    }

    public static ChatRes unsent(ModelRes res) {
        return new ChatRes(res.content(), res.reasoning(), res.usage(), false);
    }

    public static ChatRes sent(ModelUsage usage) {
        return new ChatRes(null, null, usage, true);
    }
}
