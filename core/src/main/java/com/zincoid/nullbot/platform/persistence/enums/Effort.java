package com.zincoid.nullbot.platform.persistence.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * 推理强度，持久化与序列化均为数字：0=MINIMAL 到 6=ULTRA。
 * <p>Reasoning effort persisted and serialized as a number: 0=MINIMAL through 6=ULTRA.</p>
 */
@Getter
public enum Effort {

    MINIMAL(0, "minimal"),
    LOW(1, "low"),
    MEDIUM(2, "medium"),
    HIGH(3, "high"),
    XHIGH(4, "xhigh"),
    MAX(5, "max"),
    ULTRA(6, "ultra");

    @EnumValue
    private final Integer value;
    private final String label;

    Effort(Integer value, String label) {
        this.value = value;
        this.label = label;
    }

    @JsonValue
    public Integer getValue() {
        return value;
    }

    @JsonCreator
    public static Effort fromValue(Integer value) {
        if (value == null) return null;
        for (Effort effort : values())
            if (effort.value.equals(value)) return effort;
        return null;
    }

    public static Effort fromLabel(String label) {
        if (label == null) return null;
        String normalized = label.trim();
        for (Effort effort : values())
            if (effort.label.equalsIgnoreCase(normalized)) return effort;
        return null;
    }
}
