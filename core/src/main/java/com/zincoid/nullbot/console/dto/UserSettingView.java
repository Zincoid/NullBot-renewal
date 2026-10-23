package com.zincoid.nullbot.console.dto;

import com.zincoid.nullbot.platform.persistence.enums.Effort;
import com.zincoid.nullbot.platform.persistence.po.UserSetting;

/**
 * 控制台用户设置视图。
 * <p>Console view of user settings.</p>
 *
 * @param userId           the platform user id
 * @param aiPrompt         the private-chat AI prompt
 * @param aiEffort         the AI reasoning effort
 * @param aiInjectionGuard whether anti prompt injection is enabled
 */
public record UserSettingView(String userId, String aiPrompt, Effort aiEffort, boolean aiInjectionGuard) {

    /**
     * 由用户设置构建视图。
     * <p>Builds the view from user settings.</p>
     *
     * @param userId  the platform user id
     * @param setting the user settings
     * @return the view
     */
    public static UserSettingView of(String userId, UserSetting setting) {
        Effort effort = setting == null || setting.getAiEffort() == null
                ? Effort.HIGH
                : setting.getAiEffort();
        String prompt = setting == null || setting.getAiPrompt() == null ? "" : setting.getAiPrompt();
        boolean guard = setting == null || !Boolean.FALSE.equals(setting.getAiInjectionGuard());
        return new UserSettingView(userId, prompt, effort, guard);
    }
}
