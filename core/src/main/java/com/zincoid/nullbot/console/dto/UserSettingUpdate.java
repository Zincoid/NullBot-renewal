package com.zincoid.nullbot.console.dto;

import com.zincoid.nullbot.platform.persistence.enums.Effort;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 用户设置更新请求。
 * <p>Request to update user settings.</p>
 *
 * @param aiPrompt         the private-chat AI prompt, blank clears it
 * @param aiEffort         the AI reasoning effort
 * @param aiInjectionGuard whether anti prompt injection is enabled
 */
public record UserSettingUpdate(
        @Size(max = 20000, message = "aiPrompt must not exceed 20000 characters")
        String aiPrompt,

        @NotNull(message = "AI effort is required")
        Effort aiEffort,

        @NotNull(message = "aiInjectionGuard is required")
        Boolean aiInjectionGuard
) {
}
