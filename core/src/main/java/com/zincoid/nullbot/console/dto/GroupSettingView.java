package com.zincoid.nullbot.console.dto;

import com.zincoid.nullbot.platform.persistence.enums.Effort;
import com.zincoid.nullbot.platform.persistence.enums.Language;
import com.zincoid.nullbot.platform.persistence.enums.LimitScope;
import com.zincoid.nullbot.platform.persistence.po.GroupSetting;

/**
 * 控制台群设置视图。
 * <p>Console view of group settings.</p>
 *
 * @param groupId             the platform group id
 * @param language            the reply language
 * @param joinNoticeEnabled   whether join notices are enabled
 * @param leaveNoticeEnabled  whether leave notices are enabled
 * @param antiRecallEnabled   whether anti-recall is enabled
 * @param limitEnabled        whether rate limiting is enabled
 * @param limitScope          the rate limit scope
 * @param limitCapacity       the rate limit capacity
 * @param limitRefill         the rate limit refill amount
 * @param limitInterval       the rate limit refill interval in minutes
 * @param aiEffort            the AI reasoning effort
 * @param aiPrompt            the group system prompt, blank when unset
 * @param aiInjectionGuard    whether anti prompt injection is enabled
 * @param aiAutoChatEnabled   whether AI replies to ordinary group messages automatically
 * @param aiChatFrequency     the auto-chat probability per message, 0..1
 */
public record GroupSettingView(
        String groupId,
        Language language,
        boolean joinNoticeEnabled,
        boolean leaveNoticeEnabled,
        boolean antiRecallEnabled,
        boolean limitEnabled,
        LimitScope limitScope,
        int limitCapacity,
        int limitRefill,
        int limitInterval,
        Effort aiEffort,
        String aiPrompt,
        boolean aiInjectionGuard,
        boolean aiAutoChatEnabled,
        double aiChatFrequency
) {

    /**
     * 由群设置记录构建视图。
     * <p>Builds the view from group settings.</p>
     *
     * @param setting the group settings
     * @return the view
     */
    public static GroupSettingView of(GroupSetting setting) {
        return new GroupSettingView(
                setting.getGroupId(),
                setting.getLanguage(),
                setting.getJoinNoticeEnabled(),
                setting.getLeaveNoticeEnabled(),
                setting.getAntiRecallEnabled(),
                setting.getLimitEnabled(),
                setting.getLimitScope(),
                setting.getLimitCapacity(),
                setting.getLimitRefill(),
                setting.getLimitInterval(),
                setting.getAiEffort(),
                setting.getAiPrompt() == null ? "" : setting.getAiPrompt(),
                setting.getAiInjectionGuard(),
                setting.getAiAutoChatEnabled(),
                setting.getAiChatFrequency());
    }
}
