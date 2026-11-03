package com.zincoid.nullbot.platform.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zincoid.nullbot.platform.persistence.enums.AiSource;
import com.zincoid.nullbot.platform.persistence.enums.ChatType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * AI 使用记录：按天汇总的触发、会话、调用次数与 token 消耗。
 * <p>AI usage record: daily aggregated trigger, chat, turn count, and token consumption.</p>
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@TableName("ai_usage")
public class AiUsage {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private LocalDate time;
    private ChatType chatType;
    private String targetId;
    private String userId;
    private AiSource source;
    private Long count;
    private Long inputTokens;
    private Long outputTokens;
}
