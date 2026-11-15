package com.zincoid.nullbot.module.memory;

import com.zincoid.nullbot.platform.persistence.po.Memory;

import java.time.LocalDateTime;
import java.util.List;

/**
 * AI 记忆服务：维护情景摘要与长期事实，并解析会话的记忆开关。
 * <p>AI memory service: maintains episodic summaries and long-term facts, and resolves the memory switches per chat.</p>
 */
public interface MemoryService {

    // ════ Policy ═════════════════

    /**
     * 判断会话是否启用情景记忆；未知会话或读取设置失败时视为禁用。
     * <p>Checks whether episodic memory is enabled for the chat; unknown chats or settings failures are treated as disabled.</p>
     *
     * @param chatId composite chat id
     * @return true when episodic memory is enabled
     */
    boolean isEpisodicEnabled(String chatId);

    /**
     * 判断会话是否启用长期记忆；未知会话或读取设置失败时视为禁用。
     * <p>Checks whether semantic memory is enabled for the chat; unknown chats or settings failures are treated as disabled.</p>
     *
     * @param chatId composite chat id
     * @return true when semantic memory is enabled
     */
    boolean isSemanticEnabled(String chatId);

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
     * 保存会话的情景摘要，已有摘要时覆盖内容、保留创建时间并刷新更新时间。
     * <p>Saves the episodic summary of the chat, overwriting the content, keeping the creation time, and refreshing the update time when a summary exists.</p>
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
     * 读取会话的语义记忆，最多返回 500 条，供控制台管理页使用。
     * <p>Reads the semantic memories of the chat, returning at most 500 records for the console page.</p>
     *
     * @param chatId chat identifier
     * @return the memories, newest first
     */
    List<Memory> listSemantic(String chatId);

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

    // ════ Chats ═══════════════════

    /**
     * 列出有记忆的会话，按最近记忆更新时间倒序。
     * <p>Lists chats that have memories, newest memory update first.</p>
     *
     * @return the chat summaries
     */
    List<ChatSummary> chats();

    /**
     * 会话记忆摘要：会话标识、情景摘要数、长期事实数与最近更新时间。
     * <p>Chat memory summary: chat identifier, episodic count, semantic count, and latest update time.</p>
     *
     * @param chatId   the composite chat id
     * @param episodic the episodic record count
     * @param semantic the semantic record count
     * @param lastAt   the latest memory update time
     */
    record ChatSummary(String chatId, long episodic, long semantic, LocalDateTime lastAt) {
    }
}
