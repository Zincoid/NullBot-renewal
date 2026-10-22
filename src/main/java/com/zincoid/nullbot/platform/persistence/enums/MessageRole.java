package com.zincoid.nullbot.platform.persistence.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

/**
 * 消息记录 {@code role} 承载的角色。
 * <p>Role carried by a message record's {@code role}.</p>
 */
@Getter
public enum MessageRole {

    USER(0, "user"),
    ASSISTANT(1, "assistant");

    @EnumValue
    private final Integer value;
    private final String label;

    MessageRole(Integer value, String label) {
        this.value = value;
        this.label = label;
    }
}
