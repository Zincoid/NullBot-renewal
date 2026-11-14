package com.zincoid.nullbot.console.dto;

import java.util.List;

/**
 * 控制台单个会话的记忆详情。
 * <p>Console memory detail of one chat.</p>
 *
 * @param chatId   the composite chat id
 * @param episodic the episodic summary, null when absent
 * @param semantic the long-term facts, newest first
 */
public record MemoryDetailView(String chatId, MemoryView episodic, List<MemoryView> semantic) {
}
