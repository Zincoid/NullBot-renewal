package com.zincoid.nullbot.platform.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 私聊指令禁用规则。
 * <p>Private-chat command disable rule.</p>
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@TableName("user_cmd_disable")
public class UserCmdDisable {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private String userId;
    private String cmd;
    private LocalDateTime expiresAt;
}
