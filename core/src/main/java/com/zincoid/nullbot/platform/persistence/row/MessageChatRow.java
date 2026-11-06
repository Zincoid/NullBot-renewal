package com.zincoid.nullbot.platform.persistence.row;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 消息会话汇总查询行。
 * <p>Message chat summary query row.</p>
 */
@Data
public class MessageChatRow {

    private String chatId;
    private Long messages;
    private LocalDateTime lastAt;
}
