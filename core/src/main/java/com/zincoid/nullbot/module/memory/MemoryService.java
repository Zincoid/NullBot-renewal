package com.zincoid.nullbot.module.memory;

import com.zincoid.nullbot.platform.persistence.po.Memory;

import java.util.List;

/**
 * AI 记忆服务：维护情景摘要与长期事实。
 * <p>AI memory service: maintains episodic summaries and long-term facts.</p>
 */
public interface MemoryService {

    // ════ Episodic ════════════════

    /**
     * 读取会话的情景摘要。
     * <p>Reads the episodic summary of the chat.</p>
     *
     * @param chatId chat identifier
     * @return the summary, or null when absent
     */
    Memory getEpisodic(String chatId);

    /**
     * 保存会话的情景摘要，已有摘要时覆盖内容并保留创建时间。
     * <p>Saves the episodic summary of the chat, overwriting the content and keeping the creation time when a summary exists.</p>
     *
     * @param chatId  chat identifier
     * @param content the summary content
     */
    void putEpisodic(String chatId, String content);

    /**
     * 清空会话的情景摘要。
     * <p>Clears the episodic summary of the chat.</p>
     *
     * @param chatId chat identifier
     */
    void clearEpisodic(String chatId);

    // ════ Semantic ════════════════

    /**
     * 读取最近的语义记忆。
     * <p>Reads the latest semantic memories.</p>
     *
     * @param chatId chat identifier
     * @param limit  the maximum number of records
     * @return the memories, newest first
     */
    List<Memory> latestSemantic(String chatId, int limit);

    /**
     * 在语义记忆中按关键词搜索。
     * <p>Searches semantic memories by keyword.</p>
     *
     * @param chatId  chat identifier
     * @param keyword the keyword
     * @param limit   the maximum number of records
     * @return the matching memories, newest first
     */
    List<Memory> searchSemantic(String chatId, String keyword, int limit);

    /**
     * 写入一条语义记忆，相同内容已存在时返回已有记录。
     * <p>Adds one semantic memory, returning the existing record when the same content is already present.</p>
     *
     * @param chatId  chat identifier
     * @param content the memory content
     * @return the stored memory
     */
    Memory addSemantic(String chatId, String content);

    /**
     * 删除一条语义记忆，不会删除情景摘要。
     * <p>Deletes one semantic memory; episodic summaries are never deleted.</p>
     *
     * @param chatId chat identifier
     * @param id     the memory id
     * @return true when the memory was deleted
     */
    boolean deleteSemantic(String chatId, long id);

    /**
     * 清空会话的长期事实。
     * <p>Clears the long-term facts of the chat.</p>
     *
     * @param chatId chat identifier
     */
    void clearSemantic(String chatId);
}
