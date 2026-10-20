package com.zincoid.nullbot.platform.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zincoid.nullbot.platform.persistence.enums.BizType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("file")
public class File {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private String userId;
    private String original;
    private String stored;
    private String description;
    private Long size;
    private BizType type;
    private LocalDateTime createdAt;
}
