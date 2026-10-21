package com.zincoid.nullbot.module.ai.model;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * 推理强度：minimal、low、medium、high、xhigh、max、ultra。
 * <p>Reasoning effort: minimal, low, medium, high, xhigh, max, or ultra.</p>
 */
@Getter
public enum Effort {

    MINIMAL("minimal"),
    LOW("low"),
    MEDIUM("medium"),
    HIGH("high"),
    XHIGH("xhigh"),
    MAX("max"),
    ULTRA("ultra");

    @JsonValue
    private final String value;

    Effort(String value) {
        this.value = value;
    }
}
