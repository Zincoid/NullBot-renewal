package com.zincoid.nullbot.console.dto;

import java.time.LocalDateTime;

/**
 * 控制台记忆会话视图。
 * <p>Console view of a chat that has AI memories.</p>
 *
 * @param chatId   the composite chat id
 * @param name     the display name
 * @param type     the chat type label
 * @param episodic the episodic record count
 * @param semantic the semantic record count
 * @param lastAt   the latest memory update time
 */
public record MemoryChatView(
        String chatId,
        String name,
        String type,
        long episodic,
        long semantic,
        LocalDateTime lastAt
) {
}
