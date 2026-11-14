package com.zincoid.nullbot.platform.persistence.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * 记忆记录 {@code type} 承载的类型。
 * <p>Type carried by a memory record's {@code type}.</p>
 */
@Getter
public enum MemoryType {

    EPISODIC(0, "episodic"),
    SEMANTIC(1, "semantic");

    @EnumValue
    private final Integer value;
    private final String label;

    MemoryType(Integer value, String label) {
        this.value = value;
        this.label = label;
    }

    @JsonValue
    public Integer getValue() {
        return value;
    }

    @JsonCreator
    public static MemoryType fromValue(Integer value) {
        if (value == null) return null;
        for (MemoryType type : values())
            if (type.value.equals(value)) return type;
        return null;
    }
}
