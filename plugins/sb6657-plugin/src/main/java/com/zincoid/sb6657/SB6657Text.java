package com.zincoid.sb6657;

import com.zincoid.nullbot.platform.persistence.enums.Language;

import java.util.Locale;

/**
 * SB6657 插件文案：中文与英文成对保存。
 * <p>SB6657 plugin texts: Chinese and English stored in pairs.</p>
 */
public enum SB6657Text {

    HELP("""
            SB6657
            随机发送一条玩机器烂梗。
            用法：SB6657
            别名：6657/玩机器/玩机器烂梗
            权限：>=0(normal)""", """
            SB6657
            Sends a random 玩机器 meme.
            Usage: SB6657
            Aliases: 6657/玩机器/玩机器烂梗
            Access: >=0(normal)"""),

    INTERRUPTED("请求被中断", "Request interrupted"),
    REQUEST_FAILED("接口请求失败", "API request failed");

    private final String zh;
    private final String en;

    SB6657Text(String zh, String en) {
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
