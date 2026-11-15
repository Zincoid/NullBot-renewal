package com.zincoid.nullbot.console.dto;

import com.zincoid.nullbot.platform.persistence.enums.Effort;
import com.zincoid.nullbot.platform.persistence.po.UserSetting;

/**
 * 控制台用户设置视图。
 * <p>Console view of user settings.</p>
 *
 * @param userId                       the platform user id
 * @param privateAiPrompt              the private-chat AI prompt
 * @param privateAiEffort              the AI reasoning effort
 * @param privateAiInjectionGuardEnabled whether anti prompt injection is enabled
 * @param privateAiEpisodicMemoryEnabled whether episodic memory is enabled
 * @param privateAiSemanticMemoryEnabled whether semantic memory is enabled
 */
public record UserSettingView(String userId, String privateAiPrompt,
                              Effort privateAiEffort, boolean privateAiInjectionGuardEnabled,
                              boolean privateAiEpisodicMemoryEnabled,
                              boolean privateAiSemanticMemoryEnabled) {

    /**
     * 由用户设置构建视图。
     * <p>Builds the view from user settings.</p>
     *
     * @param userId  the platform user id
     * @param setting the user settings
     * @return the view
     */
    public static UserSettingView of(String userId, UserSetting setting) {
        return new UserSettingView(
                userId,
                setting.getPrivateAiPrompt() == null ? "" : setting.getPrivateAiPrompt(),
                setting.getPrivateAiEffort(),
                setting.getPrivateAiInjectionGuardEnabled(),
                setting.getPrivateAiEpisodicMemoryEnabled(),
                setting.getPrivateAiSemanticMemoryEnabled());
    }
}
