package com.zincoid.nullbot.platform.persistence.row;

import lombok.Data;

/**
 * AI 用量汇总查询行。
 * <p>AI usage summary query row.</p>
 */
@Data
public class AiUsageSummaryRow {

    private Long total;
    private Long inputTokens;
    private Long outputTokens;
    private Long users;
    private Long groups;
}
