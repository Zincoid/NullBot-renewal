package com.zincoid.gallery;

import com.zincoid.nullbot.platform.persistence.enums.Language;

import java.util.Locale;

/**
 * Gallery 插件文案：中文与英文成对保存。
 * <p>Gallery plugin texts: Chinese and English stored in pairs.</p>
 */
public enum GalleryText {

    ANIME_HELP("""
            Anime
            随机发送一张二次元图片。
            用法：Anime
            别名：二次元
            权限：>=0(normal)""", """
            Anime
            Sends a random anime image.
            Usage: Anime
            Aliases: 二次元
            Access: >=0(normal)"""),

    ZINCOID_HELP("""
            Zincoid
            随机发送一张 Zincoid 女装照片。
            用法：Zincoid/zn/锌
            权限：>=2(admin)""", """
            Zincoid
            Sends a random Zincoid's photo.
            Usage: Zincoid/zn/锌
            Access: >=2(admin)""");

    private final String zh;
    private final String en;

    GalleryText(String zh, String en) {
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
