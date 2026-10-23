package com.zincoid.nullbot.platform.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zincoid.nullbot.platform.persistence.enums.Effort;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@TableName("user_setting")
public class UserSetting {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private String userId;
    private String aiPrompt;
    private Effort aiEffort;
    private Boolean aiInjectionGuard;
}
