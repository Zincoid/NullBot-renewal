package com.zincoid.hello;

import com.zincoid.nullbot.platform.persistence.enums.Language;

import java.util.Locale;

/**
 * Hello 插件文案：中文与英文成对保存。
 * <p>Hello plugin texts: Chinese and English stored in pairs.</p>
 */
public enum HelloText {

    HELP("""
            Hello
            发送示例消息；使用 --image 输出图片卡片。
            用法：Hello [--image|-i]
            别名：你好
            权限：>=0(normal)""", """
            Hello
            Sends a hello message, or an image card with --image.
            Usage: Hello [--image|-i]
            Aliases: 你好
            Access: >=0(normal)"""),

    IMAGE_FAILED("Hello 图片生成失败", "Failed to render the hello image");

    private final String zh;
    private final String en;

    HelloText(String zh, String en) {
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
