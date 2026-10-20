package com.zincoid.nullbot.platform.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("message")
public class Message {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private String userId;
    private String replyId;
    private String content;
    private String files;
    private LocalDateTime createdAt;
}
