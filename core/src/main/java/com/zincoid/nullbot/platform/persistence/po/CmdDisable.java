package com.zincoid.nullbot.platform.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zincoid.nullbot.platform.persistence.enums.ChatType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 指令禁用规则：按会话类型区分群聊与私聊。
 * <p>Command disable rule: distinguishes group chats from private chats by chat type.</p>
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@TableName("cmd_disable")
public class CmdDisable {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private ChatType chatType;
    private String chatId;
    private String userId;
    private String cmd;
    private LocalDateTime expiresAt;
}
