package com.zincoid.nullbot.platform.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zincoid.nullbot.platform.persistence.enums.ChatType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 指令使用记录：按天汇总的会话与调用次数。
 * <p>Command usage record: daily aggregated chat and call count.</p>
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@TableName("`usage`")
public class Usage {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private LocalDate time;
    private ChatType chatType;
    private String chatId;
    private String userId;
    private String cmd;
    private Long count;
}
