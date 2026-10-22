package com.zincoid.nullbot.console.dto;

/**
 * 用户设置更新请求。
 * <p>Request to update user settings.</p>
 *
 * @param aiPrompt the private-chat AI prompt
 */
public record UserSettingUpdate(
        String aiPrompt
) {
}
