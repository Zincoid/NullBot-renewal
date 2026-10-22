package com.zincoid.nullbot.console.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 文件信息更新请求。
 * <p>Request to update file information.</p>
 *
 * @param original    the new original filename
 * @param description the new description, blank clears it
 */
public record FileUpdate(
        @NotBlank(message = "Original filename is required")
        @Size(max = 255, message = "Original filename must not exceed 255 characters")
        String original,

        @Size(max = 255, message = "Description must not exceed 255 characters")
        String description
) {
}
