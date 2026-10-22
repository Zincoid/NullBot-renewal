package com.zincoid.nullbot.platform.persistence.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * 推理强度，持久化为数字，序列化为小写字符串：minimal、low、medium、high、xhigh、max、ultra。
 * <p>Reasoning effort persisted as a number and serialized as a lowercase string: minimal, low, medium, high, xhigh, max, or ultra.</p>
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
    @JsonValue
    private final String label;

    Effort(Integer value, String label) {
        this.value = value;
        this.label = label;
    }
}
