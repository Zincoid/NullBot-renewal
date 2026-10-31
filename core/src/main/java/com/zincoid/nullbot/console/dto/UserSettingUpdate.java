package com.zincoid.nullbot.console.dto;

import com.zincoid.nullbot.platform.persistence.enums.Effort;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 用户设置更新请求。
 * <p>Request to update user settings.</p>
 *
 * @param privateAiPrompt         the private-chat AI prompt, blank clears it
 * @param privateAiEffort         the AI reasoning effort
 * @param privateAiInjectionGuard whether anti prompt injection is enabled
 */
public record UserSettingUpdate(
        @Size(max = 20000, message = "privateAiPrompt must not exceed 20000 characters")
        String privateAiPrompt,

        @NotNull(message = "AI effort is required")
        Effort privateAiEffort,

        @NotNull(message = "privateAiInjectionGuard is required")
        Boolean privateAiInjectionGuard
) {
}
