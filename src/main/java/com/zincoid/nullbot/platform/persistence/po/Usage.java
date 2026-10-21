package com.zincoid.nullbot.platform.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@TableName("`usage`")
public class Usage {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private LocalDate time;
    private String groupId;
    private String userId;
    private String cmd;
    private Long count;
}
