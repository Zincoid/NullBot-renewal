package com.zincoid.nullbot.platform.persistence.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

/**
 * 文件记录 {@code type} 承载的业务类型。
 * <p>Business type carried by a file record's {@code type}.</p>
 */
@Getter
public enum BizType {

    IMG_COLL(0, "Image collection"),
    VID_COLL(1, "Video collection"),
    AUD_COLL(2, "Audio collection"),
    CHAT(3, "Chat attachment");

    @EnumValue
    private final Integer value;
    private final String description;

    BizType(Integer value, String description) {
        this.value = value;
        this.description = description;
    }
}
