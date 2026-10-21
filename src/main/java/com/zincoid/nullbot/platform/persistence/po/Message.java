package com.zincoid.nullbot.platform.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.zincoid.nullbot.platform.persistence.enums.MessageRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@TableName(value = "message", autoResultMap = true)
public class Message {

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String chatId;
    private String userId;
    private MessageRole role;
    private String nickname;
    private String content;
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> files;
    private LocalDateTime createdAt;
}
