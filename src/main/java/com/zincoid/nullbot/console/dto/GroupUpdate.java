package com.zincoid.nullbot.console.dto;

import com.zincoid.nullbot.platform.persistence.enums.Access;
import jakarta.validation.constraints.NotNull;

/**
 * 群权限更新请求。
 * <p>Request to update a group's access level.</p>
 *
 * @param access the new access level
 */
public record GroupUpdate(
        @NotNull(message = "Access is required")
        Access access
) {
}
