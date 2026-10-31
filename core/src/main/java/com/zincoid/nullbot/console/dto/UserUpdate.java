package com.zincoid.nullbot.console.dto;

import com.zincoid.nullbot.platform.persistence.enums.Access;
import jakarta.validation.constraints.NotNull;

/**
 * 用户权限更新请求。
 * <p>Request to update a user's permissions.</p>
 *
 * @param access               the new access level
 * @param privateCmdEnabled    whether private commands are enabled, nullable to keep the current value
 * @param privateAiChatEnabled whether private AI chat is enabled, nullable to keep the current value
 */
public record UserUpdate(
        @NotNull(message = "Access is required")
        Access access,
        Boolean privateCmdEnabled,
        Boolean privateAiChatEnabled
) {
}
