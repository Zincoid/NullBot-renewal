package com.zincoid.nullbot.platform.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zincoid.nullbot.platform.persistence.enums.MessageRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@TableName("message")
public class Message {

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String chatId;
    private String userId;
    private MessageRole role;
    private String nickname;
    private String content;
    private String reasoning;
    private LocalDateTime createdAt;
}
