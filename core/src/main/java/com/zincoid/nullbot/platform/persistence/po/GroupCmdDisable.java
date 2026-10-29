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
 * 群聊指令禁用规则：整群或群内指定用户。
 * <p>Group command disable rule: a whole group or one member of it.</p>
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@TableName("group_cmd_disable")
public class GroupCmdDisable {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private String groupId;
    private String userId;
    private String cmd;
    private LocalDateTime expiresAt;
}
