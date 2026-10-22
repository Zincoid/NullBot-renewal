package com.zincoid.nullbot.platform.persistence.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * 用户或群组的限权等级。
 * <p>Access level of a user or group.</p>
 */
@Getter
public enum Access {

    BANNED(-2, "banned"),
    RESTRICTED(-1, "restricted"),
    NORMAL(0, "normal"),
    MANAGER(1, "manager"),
    ADMIN(2, "admin");

    @EnumValue
    private final Integer value;
    private final String label;

    Access(Integer value, String label) {
        this.value = value;
        this.label = label;
    }

    @JsonValue
    public Integer getValue() {
        return value;
    }

    @JsonCreator
    public static Access fromValue(Integer value) {
        if (value == null) return null;
        for (Access access : values())
            if (access.value.equals(value)) return access;
        return null;
    }
}
