package com.zincoid.nullbot.platform.persistence.row;

import lombok.Data;

/**
 * AI 用量排行查询行。
 * <p>AI usage ranking query row.</p>
 */
@Data
public class AiUsageRankRow {

    private String key;
    private Long count;
    private Long inputTokens;
    private Long outputTokens;
}
