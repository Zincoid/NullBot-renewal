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
public enum AiTrigger {

    GROUP_CHAT(0, "group_chat"),
    GROUP_AUTO_CHAT(1, "group_auto_chat"),
    GROUP_POKE(2, "group_poke"),
    PRIVATE_CHAT(3, "private_chat"),
    PRIVATE_POKE(4, "private_poke");

    @EnumValue
    private final Integer value;
    private final String label;

    AiTrigger(Integer value, String label) {
        this.value = value;
        this.label = label;
    }

    @JsonValue
    public Integer getValue() {
        return value;
    }

    @JsonCreator
    public static AiTrigger fromValue(Integer value) {
        if (value == null) return null;
        for (AiTrigger trigger : values())
            if (trigger.value.equals(value)) return trigger;
        return null;
    }
}
