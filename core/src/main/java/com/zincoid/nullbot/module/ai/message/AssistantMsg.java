package com.zincoid.nullbot.module.ai.message;

import com.zincoid.nullbot.module.ai.tool.ToolCall;

import java.util.List;

/**
 * 助手消息。
 * <p>Assistant message.</p>
 *
 * @param content   the message text, nullable when only tool calls are present
 * @param tcs       the tool calls, never null
 * @param reasoning the reasoning content, nullable
 */
public record AssistantMsg(String content, List<ToolCall> tcs, String reasoning)
        implements AiMsg {

    public AssistantMsg {
        tcs = tcs == null ? List.of() : List.copyOf(tcs);
    }

    /**
     * 复制并附加推理内容。
     * <p>Returns a copy with the reasoning content attached.</p>
     *
     * @param reasoning the reasoning content, nullable
     * @return the assistant message copy
     */
    public AssistantMsg withReasoning(String reasoning) {
        return new AssistantMsg(content, tcs, reasoning);
    }
}
