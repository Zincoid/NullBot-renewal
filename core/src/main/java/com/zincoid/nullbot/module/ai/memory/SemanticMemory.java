package com.zincoid.nullbot.module.ai.memory;

import com.zincoid.nullbot.module.ai.context.ContextPacket;
import com.zincoid.nullbot.platform.persistence.po.Memory;

import java.util.List;

/**
 * 语义记忆：维护可增删查的长期事实，并注入后续对话。
 * <p>Semantic memory: maintains searchable long-term facts and injects them into later turns.</p>
 */
public interface SemanticMemory {

    /**
     * 读取会话的长期事实上下文。
     * <p>Returns the long-term fact context of the chat.</p>
     *
     * @param chatId chat identifier
     * @return the context packets, empty when no fact exists
     */
    List<ContextPacket> context(String chatId);

    /**
     * 搜索长期事实，关键词为空时返回最近的事实。
     * <p>Searches long-term facts, returning the latest facts when the keyword is blank.</p>
     *
     * @param chatId  chat identifier
     * @param keyword the keyword, nullable
     * @param limit   the maximum number of records
     * @return the matching facts, newest first
     */
    List<Memory> search(String chatId, String keyword, int limit);

    /**
     * 写入一条长期事实。
     * <p>Adds one long-term fact.</p>
     *
     * @param chatId  chat identifier
     * @param content the fact content
     * @return the stored fact
     */
    Memory add(String chatId, String content);

    /**
     * 删除一条长期事实。
     * <p>Deletes one long-term fact.</p>
     *
     * @param chatId chat identifier
     * @param id     the memory id
     * @return true when the fact was deleted
     */
    boolean delete(String chatId, long id);
}
