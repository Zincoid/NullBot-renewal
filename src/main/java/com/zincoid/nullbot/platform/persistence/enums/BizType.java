package com.zincoid.nullbot.platform.persistence.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
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
    CHAT(3, "chat");

    @EnumValue
    private final Integer value;
    private final String label;

    BizType(Integer value, String label) {
        this.value = value;
        this.label = label;
    }
}
