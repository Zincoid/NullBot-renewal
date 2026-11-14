package com.zincoid.nullbot.platform.persistence.row;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 记忆会话汇总查询行。
 * <p>Memory chat summary query row.</p>
 */
@Data
public class MemoryChatRow {

    private String chatId;
    private Long episodic;
    private Long semantic;
    private LocalDateTime lastAt;
}
