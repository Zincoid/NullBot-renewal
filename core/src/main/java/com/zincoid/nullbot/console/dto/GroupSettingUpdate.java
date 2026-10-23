package com.zincoid.nullbot.console.dto;

import com.zincoid.nullbot.platform.persistence.enums.Effort;
import com.zincoid.nullbot.platform.persistence.enums.Language;
import com.zincoid.nullbot.platform.persistence.enums.LimitScope;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * 群设置更新请求。
 * <p>Request to update group settings.</p>
 *
 * @param language           the reply language
 * @param joinNoticeEnabled  whether join notices are enabled
 * @param leaveNoticeEnabled whether leave notices are enabled
 * @param limitEnabled       whether rate limiting is enabled
 * @param limitScope         the rate limit scope
 * @param limitCapacity      the rate limit capacity
 * @param limitRefill        the rate limit refill amount
 * @param limitInterval      the rate limit refill interval in minutes
 * @param aiEffort           the AI reasoning effort
 * @param aiPrompt           the group system prompt, blank clears it
 * @param aiInjectionGuard   whether anti prompt injection is enabled
 * @param aiAutoChatEnabled  whether AI replies to ordinary group messages automatically
 * @param aiChatFrequency    the auto-chat probability per message, 0..1
 */
public record GroupSettingUpdate(
        @NotNull(message = "Language is required")
        Language language,

        @NotNull(message = "joinNoticeEnabled is required")
        Boolean joinNoticeEnabled,

        @NotNull(message = "leaveNoticeEnabled is required")
        Boolean leaveNoticeEnabled,

        @NotNull(message = "limitEnabled is required")
        Boolean limitEnabled,

        @NotNull(message = "Limit scope is required")
        LimitScope limitScope,

        @NotNull(message = "limitCapacity is required")
        @Positive(message = "limitCapacity must be positive")
        Integer limitCapacity,

        @NotNull(message = "limitRefill is required")
        @Positive(message = "limitRefill must be positive")
        Integer limitRefill,

        @NotNull(message = "limitInterval is required")
        @Positive(message = "limitInterval must be positive")
        Integer limitInterval,

        @NotNull(message = "AI effort is required")
        Effort aiEffort,

        @Size(max = 20000, message = "aiPrompt must not exceed 20000 characters")
        String aiPrompt,

        @NotNull(message = "aiInjectionGuard is required")
        Boolean aiInjectionGuard,

        @NotNull(message = "aiAutoChatEnabled is required")
        Boolean aiAutoChatEnabled,

        @NotNull(message = "aiChatFrequency is required")
        @DecimalMin(value = "0.0", message = "aiChatFrequency must be between 0 and 1")
        @DecimalMax(value = "1.0", message = "aiChatFrequency must be between 0 and 1")
        Double aiChatFrequency
) {
}
