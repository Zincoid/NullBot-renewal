package com.zincoid.nullbot.console.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 控制台主动发送消息请求。
 * <p>Request to send a message from the console.</p>
 *
 * @param chatId  the composite chat id
 * @param content the text content
 */
public record MessageSend(
        @NotBlank(message = "Chat id is required")
        @Size(max = 64, message = "Chat id must not exceed 64 characters")
        String chatId,

        @NotBlank(message = "Content is required")
        @Size(max = 4000, message = "Content must not exceed 4000 characters")
        String content
) {
}
