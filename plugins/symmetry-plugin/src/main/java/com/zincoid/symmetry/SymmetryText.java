package com.zincoid.symmetry;

import com.zincoid.nullbot.platform.persistence.enums.Language;

import java.util.Locale;

/**
 * Symmetry 插件文案：中文与英文成对保存。
 * <p>Symmetry plugin texts: Chinese and English stored in pairs.</p>
 */
public enum SymmetryText {

    HELP("""
            Symmetry
            将图片的一半镜像到另一侧。
            用法：
            1. [引用图片] Symmetry [方式]
            2. Symmetry [方式] [附加图片]
            3. Symmetry [方式] <QQ号 | @用户>
            方式：左/右/上/下（默认左）
            别名：图像对称/对称
            权限：>=0(normal)""", """
            Symmetry
            Mirrors one half of an image onto the other side.
            Usage:
            1. [reply image] Symmetry [mode]
            2. Symmetry [mode] [attached image]
            3. Symmetry [mode] <QQ id | @user>
            Mode: left/right/top/bottom (default left)
            Aliases: 图像对称/对称
            Access: >=0(normal)"""),

    NO_IMAGE_IN_REPLY("引用中没有图片", "No image in the reply"),
    MISSING_SOURCE("缺少图片引用/ID/@用户", "Missing image reply, id, or mention"),
    IMAGE_FAILED("图片处理失败", "Image processing failed"),
    READ_FAILED("无法读取引用消息", "Cannot read the replied message"),
    AVATAR_FAILED("无法获取用户头像", "Cannot fetch the user avatar"),
    NO_AVATAR("该用户没有头像", "The user has no avatar");

    private final String zh;
    private final String en;

    SymmetryText(String zh, String en) {
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
