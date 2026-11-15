package com.zincoid.nullbot.module.ai.memory;

import com.zincoid.nullbot.module.ai.context.ContextPacket;

import java.util.List;

/**
 * 情景记忆：把待压缩的已读消息压缩为滚动摘要，并注入后续对话。
 * <p>Episodic memory: compacts pending read messages into a rolling summary and injects it into later turns.</p>
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
     * 压缩待处理的已读消息，优先压缩最老的一半。
     * <p>Compacts pending read messages, preferring the oldest half.</p>
     *
     * @param chatId chat identifier
     */
    void compact(String chatId);
}
