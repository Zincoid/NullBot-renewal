package com.zincoid.nullbot.console.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 私聊指令禁用规则创建请求。
 * <p>Request to create a private-chat command-disable rule.</p>
 *
 * @param cmd      the command name
 * @param duration the duration such as {@code 30m}, or null for a permanent rule
 */
public record PrivateCmdDisableCreate(
        @NotBlank(message = "Command is required")
        @Size(max = 100, message = "Command must not exceed 100 characters")
        String cmd,

        @Size(max = 20, message = "Duration must not exceed 20 characters")
        String duration
) {
}
