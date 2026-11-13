package com.zincoid.nullbot.module.ai.message;

import com.zincoid.nullbot.module.ai.AiException;

/**
 * 工具结果消息。
 * <p>Tool result message.</p>
 *
 * @param tcId    the tool call id, required
 * @param content the tool result text
 */
public record ToolMsg(String tcId, String content) implements AiMsg {

    public ToolMsg {
        if (tcId == null || tcId.isBlank())
            throw new AiException("Missing tool call id");
    }
}
