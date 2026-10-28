package com.zincoid.pubg;

import com.zincoid.nullbot.platform.persistence.enums.Language;

import java.util.Locale;

/**
 * PUBG 插件文案：中文与英文成对保存。
 * <p>PUBG plugin texts: Chinese and English stored in pairs.</p>
 */
public enum PubgText {

    HELP("""
            PUBG
            发送 PUBG 资源地图。
            用法：PUBG <地图>
            地图：艾伦格/米拉玛/维寒迪/帝斯顿/荣都/泰戈
            首字母：alg/mlm/whd/dsd/rd/tg
            别名：吃鸡地图
            权限：>=0(normal)""", """
            PUBG
            Sends a PUBG resource map.
            Usage: PUBG <map>
            Maps: 艾伦格/米拉玛/维寒迪/帝斯顿/荣都/泰戈
            Initials: alg/mlm/whd/dsd/rd/tg
            Aliases: 吃鸡地图
            Access: >=0(normal)"""),

    AVAILABLE_MAPS("可用地图：%s", "Available maps: %s"),
    UNSUPPORTED_MAP("暂不支持该地图：%s", "Unsupported map: %s");

    private final String zh;
    private final String en;

    PubgText(String zh, String en) {
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
