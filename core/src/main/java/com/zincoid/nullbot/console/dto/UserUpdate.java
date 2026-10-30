package com.zincoid.nullbot.console.dto;

import com.zincoid.nullbot.platform.persistence.enums.Access;
import jakarta.validation.constraints.NotNull;

/**
 * 用户权限与私聊开关更新请求。
 * <p>Request to update a user's access level and private-message flag.</p>
 *
 * @param access                the new access level
 * @param privateMessageAllowed the new private-message flag, nullable to keep the current value
 */
public record UserUpdate(
        @NotNull(message = "Access is required")
        Access access,
        Boolean privateMessageAllowed
) {
}
