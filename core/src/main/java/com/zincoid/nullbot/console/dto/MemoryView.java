package com.zincoid.nullbot.console.dto;

import com.zincoid.nullbot.platform.persistence.po.Memory;

import java.time.LocalDateTime;

/**
 * 控制台记忆视图。
 * <p>Console view of one AI memory record.</p>
 *
 * @param id        the memory id
 * @param type      the memory type label
 * @param content   the memory content
 * @param createdAt the creation time
 * @param updatedAt the update time
 */
public record MemoryView(Long id,
                         String type,
                         String content,
                         LocalDateTime createdAt,
                         LocalDateTime updatedAt) {

    /**
     * 由记忆记录构建视图。
     * <p>Builds the view from a memory record.</p>
     *
     * @param memory the memory record
     * @return the view
     */
    public static MemoryView of(Memory memory) {
        return new MemoryView(
                memory.getId(),
                memory.getType() == null ? null : memory.getType().getLabel(),
                memory.getContent(),
                memory.getCreatedAt(),
                memory.getUpdatedAt());
    }
}
