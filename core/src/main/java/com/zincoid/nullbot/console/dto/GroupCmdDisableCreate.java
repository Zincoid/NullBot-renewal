package com.zincoid.nullbot.console.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 群聊指令禁用规则创建请求。
 * <p>Request to create a group command-disable rule.</p>
 *
 * @param userId    the target user id, {@code *} for a whole group
 * @param cmd       the command name
 * @param duration  the duration such as {@code 30m}, or null for a permanent rule
 */
public record GroupCmdDisableCreate(
        @NotBlank(message = "User is required")
        @Size(max = 64, message = "User must not exceed 64 characters")
        String userId,

        @NotBlank(message = "Command is required")
        @Size(max = 100, message = "Command must not exceed 100 characters")
        String cmd,

        @Size(max = 20, message = "Duration must not exceed 20 characters")
        String duration
) {
}
