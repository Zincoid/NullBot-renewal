package com.zincoid.nullbot.platform.persistence.row;

import com.zincoid.nullbot.platform.persistence.enums.AiSource;
import lombok.Data;

/**
 * AI 触发来源汇总查询行。
 * <p>AI trigger summary query row.</p>
 */
@Data
public class AiUsageTriggerRow {

    private AiSource source;
    private Long count;
    private Long inputTokens;
    private Long outputTokens;
}
