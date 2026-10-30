package com.zincoid.nullbot.platform.persistence.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * 会话类型：群聊或私聊。
 * <p>Chat type: group chat or private chat.</p>
 */
@Getter
public enum ChatType {

    GROUP(0, "group"),
    PRIVATE(1, "private");

    @EnumValue
    private final Integer value;
    private final String label;

    ChatType(Integer value, String label) {
        this.value = value;
        this.label = label;
    }

    @JsonValue
    public Integer getValue() {
        return value;
    }

    @JsonCreator
    public static ChatType fromValue(Integer value) {
        if (value == null) return null;
        for (ChatType type : values())
            if (type.value.equals(value)) return type;
        return null;
    }
}
