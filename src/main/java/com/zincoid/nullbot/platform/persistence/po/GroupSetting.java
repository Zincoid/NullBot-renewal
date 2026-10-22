package com.zincoid.nullbot.platform.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zincoid.nullbot.platform.persistence.enums.Effort;
import com.zincoid.nullbot.platform.persistence.enums.Language;
import com.zincoid.nullbot.platform.persistence.enums.LimitScope;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@TableName("group_setting")
public class GroupSetting {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private String groupId;
    private Language language;
    private Boolean limitEnabled;
    private LimitScope limitScope;
    private Integer limitCapacity;
    private Integer limitRefill;
    private Integer limitInterval;
    private Effort aiEffort;
    private String aiPrompt;
    private Boolean aiInjectionGuard;
}
