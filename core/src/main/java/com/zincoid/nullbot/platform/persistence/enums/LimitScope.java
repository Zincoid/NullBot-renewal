package com.zincoid.nullbot.platform.persistence.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * 限速维度：按用户、按群或按指令独立计数。
 * <p>Rate limit scope: counting per user, per group, or per command.</p>
 */
@Getter
public enum LimitScope {

    USER(0, "user"),
    GROUP(1, "group"),
    CMD(2, "cmd");

    @EnumValue
    private final Integer value;
    private final String label;

    LimitScope(Integer value, String label) {
        this.value = value;
        this.label = label;
    }

    @JsonValue
    public Integer getValue() {
        return value;
    }

    @JsonCreator
    public static LimitScope fromValue(Integer value) {
        if (value == null) return null;
        for (LimitScope scope : values())
            if (scope.value.equals(value)) return scope;
        return null;
    }

    public static LimitScope fromLabel(String label) {
        if (label == null) return null;
        for (LimitScope scope : values())
            if (scope.label.equalsIgnoreCase(label.trim())) return scope;
        return null;
    }
}
