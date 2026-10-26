package com.zincoid.say;

import com.zincoid.nullbot.platform.persistence.enums.Language;

import java.util.Locale;

/**
 * Say 插件文案：中文与英文成对保存。
 * <p>Say plugin texts: Chinese and English stored in pairs.</p>
 */
public enum SayText {

    HELP("""
            Say
            随机渲染一条语录卡片。
            用法：Say [用户ID | @用户]
                  Say [--id=语录ID | -i=语录ID]
            别名：说
            权限：>=0(normal)""", """
            Say
            Renders a random quote card.
            Usage: Say [user id | @user]
                   Say [--id=<quote id> | -i=<quote id>]
            Aliases: 说
            Access: >=0(normal)"""),

    RENDER_FAILED("语录图片生成失败", "Failed to render the quote image");

    private final String zh;
    private final String en;

    SayText(String zh, String en) {
        this.zh = zh;
        this.en = en;
    }

    /**
     * 按语言选择文案。
     * <p>Selects the text for the given language.</p>
     *
     * @param language the reply language
     * @return the localized text
     */
    public String of(Language language) {
        return language == Language.EN ? en : zh;
    }

    /**
     * 按语言选择文案并填充参数。
     * <p>Selects the text for the given language and fills its arguments.</p>
     *
     * @param language the reply language
     * @param args     the format arguments
     * @return the localized text
     */
    public String of(Language language, Object... args) {
        return String.format(Locale.ROOT, of(language), args);
    }
}
