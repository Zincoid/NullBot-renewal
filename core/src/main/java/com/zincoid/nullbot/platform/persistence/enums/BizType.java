package com.zincoid.nullbot.platform.persistence.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * 文件记录 {@code type} 承载的业务类型。
 * <p>Business type carried by a file record's {@code type}.</p>
 */
@Getter
public enum BizType {

    IMG_COLL(0, "img_coll"),
    VID_COLL(1, "vid_coll"),
    AUD_COLL(2, "aud_coll"),
    CHAT(3, "chat"),
    MEME(4, "meme");

    @EnumValue
    private final Integer value;
    private final String label;

    BizType(Integer value, String label) {
        this.value = value;
        this.label = label;
    }

    @JsonValue
    public Integer getValue() {
        return value;
    }

    @JsonCreator
    public static BizType fromValue(Integer value) {
        if (value == null) return null;
        for (BizType type : values())
            if (type.value.equals(value)) return type;
        return null;
    }

    public static BizType fromLabel(String label) {
        if (label == null) return null;
        String normalized = label.trim();
        for (BizType type : values())
            if (type.label.equalsIgnoreCase(normalized)) return type;
        return null;
    }
}
