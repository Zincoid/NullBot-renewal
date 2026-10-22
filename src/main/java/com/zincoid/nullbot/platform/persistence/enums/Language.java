package com.zincoid.nullbot.platform.persistence.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * 回复语言，持久化为数字，序列化为小写字符串：auto、zh、en。
 * <p>Reply language persisted as a number and serialized as a lowercase string: auto, zh, or en.</p>
 */
@Getter
public enum Language {

    AUTO(0, "auto"),
    ZH(1, "zh"),
    EN(2, "en");

    @EnumValue
    private final Integer value;
    @JsonValue
    private final String label;

    Language(Integer value, String label) {
        this.value = value;
        this.label = label;
    }

    /**
     * 按标签查找语言，忽略大小写。
     * <p>Finds a language by its label, ignoring case.</p>
     *
     * @param label the language label
     * @return the matching language, or null when unknown
     */
    public static Language fromLabel(String label) {
        if (label == null) return null;
        for (Language language : values())
            if (language.label.equalsIgnoreCase(label.trim())) return language;
        return null;
    }
}
