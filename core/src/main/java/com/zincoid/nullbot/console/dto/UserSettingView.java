package com.zincoid.nullbot.console.dto;

import com.zincoid.nullbot.platform.persistence.po.UserSetting;

/**
 * 控制台用户设置视图。
 * <p>Console view of user settings.</p>
 *
 * @param userId   the platform user id
 * @param aiPrompt the private-chat AI prompt
 */
public record UserSettingView(String userId, String aiPrompt) {

    /**
     * 由用户设置构建视图。
     * <p>Builds the view from user settings.</p>
     *
     * @param userId  the platform user id
     * @param setting the user settings
     * @return the view
     */
    public static UserSettingView of(String userId, UserSetting setting) {
        String prompt = setting == null || setting.getAiPrompt() == null ? "" : setting.getAiPrompt();
        return new UserSettingView(userId, prompt);
    }
}
