package com.zincoid.nullbot.platform.persistence.row;

import lombok.Data;

import java.time.LocalDate;

/**
 * 每日 AI 用量查询行。
 * <p>Daily AI usage query row.</p>
 */
@Data
public class AiUsageDailyRow {

    private LocalDate day;
    private Long count;
    private Long inputTokens;
    private Long outputTokens;
}
