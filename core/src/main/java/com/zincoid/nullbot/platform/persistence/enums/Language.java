package com.zincoid.nullbot.platform.persistence.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * 回复语言，持久化与序列化均为数字：0=AUTO、1=ZH、2=EN。
 * <p>Reply language persisted and serialized as a number: 0=AUTO, 1=ZH, 2=EN.</p>
 */
@Getter
public enum Language {

    AUTO(0, "auto"),
    ZH(1, "zh"),
    EN(2, "en");

    @EnumValue
    private final Integer value;
    private final String label;

    Language(Integer value, String label) {
        this.value = value;
        this.label = label;
    }

    @JsonValue
    public Integer getValue() {
        return value;
    }

    @JsonCreator
    public static Language fromValue(Integer value) {
        if (value == null) return null;
        for (Language language : values())
            if (language.value.equals(value)) return language;
        return null;
    }

    public static Language fromLabel(String label) {
        if (label == null) return null;
        for (Language language : values())
            if (language.label.equalsIgnoreCase(label.trim())) return language;
        return null;
    }
}
