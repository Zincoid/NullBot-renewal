package com.zincoid.nullbot.module.ai.memory;

import com.zincoid.nullbot.module.ai.context.ContextPacket;

import java.util.List;

/**
 * 情景记忆：把溢出窗口的旧消息压缩为滚动摘要，并注入后续对话。
 * <p>Episodic memory: compacts old messages beyond the window into a rolling summary and injects it into later turns.</p>
 */
public interface EpisodicMemory {

    /**
     * 读取会话的情景摘要上下文。
     * <p>Returns the episodic summary context of the chat.</p>
     *
     * @param chatId chat identifier
     * @return the context packets, empty when no summary exists
     */
    List<ContextPacket> context(String chatId);

    /**
     * 溢出消息足够多时压缩为情景摘要。
     * <p>Compacts the overflow messages into an episodic summary when enough of them accumulate.</p>
     *
     * @param chatId chat identifier
     */
    void compactIfNeeded(String chatId);
}
