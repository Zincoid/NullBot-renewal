package com.zincoid.nullbot.platform.persistence.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

/**
 * Access level of a user or group.
 * <p>用户或群组的限权等级。</p>
 */
@Getter
public enum Access {

    BANNED(-2, "Banned"),
    RESTRICTED(-1, "Restricted"),
    NORMAL(0, "Normal"),
    MANAGER(1, "Manager"),
    ADMIN(2, "Admin");

    @EnumValue
    private final Integer value;
    private final String description;

    Access(Integer value, String description) {
        this.value = value;
        this.description = description;
    }
}
