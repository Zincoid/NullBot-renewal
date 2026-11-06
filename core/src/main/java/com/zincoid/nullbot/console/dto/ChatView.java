package com.zincoid.nullbot.console.dto;

import java.time.LocalDateTime;

/**
 * 控制台会话视图。
 * <p>Console view of a chat summary.</p>
 *
 * @param chatId   the composite chat id
 * @param name     the display name
 * @param type     the chat type label
 * @param messages the retained message count
 * @param lastAt   the last message time
 */
public record ChatView(
        String chatId,
        String name,
        String type,
        long messages,
        LocalDateTime lastAt
) {
}
