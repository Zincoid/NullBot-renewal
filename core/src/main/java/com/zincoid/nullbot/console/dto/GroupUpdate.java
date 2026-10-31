package com.zincoid.nullbot.console.dto;

import com.zincoid.nullbot.platform.persistence.enums.Access;
import jakarta.validation.constraints.NotNull;

/**
 * 群权限更新请求。
 * <p>Request to update a group's permissions.</p>
 *
 * @param access        the new access level
 * @param cmdEnabled    whether commands are enabled, nullable to keep the current value
 * @param aiChatEnabled whether AI chat is enabled, nullable to keep the current value
 */
public record GroupUpdate(
        @NotNull(message = "Access is required")
        Access access,
        Boolean cmdEnabled,
        Boolean aiChatEnabled
) {
}
