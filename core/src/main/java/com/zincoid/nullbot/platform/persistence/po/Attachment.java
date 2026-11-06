package com.zincoid.nullbot.platform.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 聊天附件记录：入站消息中落盘的媒体文件。
 * <p>Chat attachment record: a media file stored from an inbound message.</p>
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@TableName("attachment")
public class Attachment {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private String messageId;
    private String userId;
    private String original;
    @TableField("`stored`")
    private String stored;
    private Long size;
    private LocalDateTime createdAt;
}
