package com.zincoid.nullbot.platform.persistence.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * AI 对话触发来源。
 * <p>AI chat trigger source.</p>
 */
@Getter
public enum AiSource {

    GROUP_CHAT(0, "group_chat"),
    GROUP_AUTO_CHAT(1, "group_auto_chat"),
    GROUP_POKE(2, "group_poke"),
    PRIVATE_CHAT(3, "private_chat"),
    PRIVATE_POKE(4, "private_poke");

    @EnumValue
    private final Integer value;
    private final String label;

    AiSource(Integer value, String label) {
        this.value = value;
        this.label = label;
    }

    @JsonValue
    public Integer getValue() {
        return value;
    }

    @JsonCreator
    public static AiSource fromValue(Integer value) {
        if (value == null) return null;
        for (AiSource source : values())
            if (source.value.equals(value)) return source;
        return null;
    }
}
