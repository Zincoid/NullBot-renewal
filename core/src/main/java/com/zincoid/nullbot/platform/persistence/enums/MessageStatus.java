package com.zincoid.nullbot.platform.persistence.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

/**
 * 消息记录 {@code status} 承载的读取与压缩状态。
 * <p>Read and compaction status carried by a message record's {@code status}.</p>
 */
@Getter
public enum MessageStatus {

    UNREAD(0, "unread"),
    READ(1, "read"),
    COMPACTED(2, "compacted");

    @EnumValue
    private final Integer value;
    private final String label;

    MessageStatus(Integer value, String label) {
        this.value = value;
        this.label = label;
    }
}
