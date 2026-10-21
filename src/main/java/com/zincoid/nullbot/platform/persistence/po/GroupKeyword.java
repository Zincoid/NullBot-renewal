package com.zincoid.nullbot.platform.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@TableName(value = "group_keyword", autoResultMap = true)
public class GroupKeyword {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private String groupId;
    private String keyword;
    private String cmd;
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> args;
    private Boolean enabled;
}
