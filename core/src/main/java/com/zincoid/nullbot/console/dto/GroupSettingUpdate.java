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
 * @param language                the reply language
 * @param joinNoticeEnabled       whether join notices are enabled
 * @param leaveNoticeEnabled      whether leave notices are enabled
 * @param antiRecallEnabled       whether anti-recall is enabled
 * @param limitEnabled            whether rate limiting is enabled
 * @param limitScope              the rate limit scope
 * @param limitCapacity           the rate limit capacity
 * @param limitRefill             the rate limit refill amount
 * @param limitInterval           the rate limit refill interval in minutes
 * @param aiPrompt                the group system prompt, blank clears it
 * @param aiEffort                the AI reasoning effort
 * @param aiAutoChatEnabled       whether AI replies to ordinary group messages automatically
 * @param aiChatFrequency         the auto-chat probability per message, 0..1
 * @param aiInjectionGuardEnabled whether anti prompt injection is enabled
 * @param aiEpisodicMemoryEnabled whether episodic memory is enabled
 * @param aiSemanticMemoryEnabled whether semantic memory is enabled
 */
public record GroupSettingUpdate(@NotNull(message = "Language is required")
                                 Language language,

                                 @NotNull(message = "joinNoticeEnabled is required")
                                 Boolean joinNoticeEnabled,

                                 @NotNull(message = "leaveNoticeEnabled is required")
                                 Boolean leaveNoticeEnabled,

                                 @NotNull(message = "antiRecallEnabled is required")
                                 Boolean antiRecallEnabled,

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

                                 @Size(max = 20000, message = "aiPrompt must not exceed 20000 characters")
                                 String aiPrompt,

                                 @NotNull(message = "AI effort is required")
                                 Effort aiEffort,

                                 @NotNull(message = "aiAutoChatEnabled is required")
                                 Boolean aiAutoChatEnabled,

                                 @NotNull(message = "aiChatFrequency is required")
                                 @DecimalMin(value = "0.0", message = "aiChatFrequency must be between 0 and 1")
                                 @DecimalMax(value = "1.0", message = "aiChatFrequency must be between 0 and 1")
                                 Double aiChatFrequency,

                                 @NotNull(message = "aiInjectionGuardEnabled is required")
                                 Boolean aiInjectionGuardEnabled,

                                 @NotNull(message = "aiEpisodicMemoryEnabled is required")
                                 Boolean aiEpisodicMemoryEnabled,

                                 @NotNull(message = "aiSemanticMemoryEnabled is required")
                                 Boolean aiSemanticMemoryEnabled) {
}
