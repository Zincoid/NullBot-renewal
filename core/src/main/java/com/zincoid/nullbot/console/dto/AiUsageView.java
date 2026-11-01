package com.zincoid.nullbot.console.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * 控制台 AI 用量视图。
 * <p>Console view of AI usage statistics.</p>
 *
 * @param days         the number of days in the range
 * @param total        the total AI turn count
 * @param inputTokens  the total input token count
 * @param outputTokens the total output token count
 * @param activeUsers  the distinct active users
 * @param activeGroups the distinct active groups, excluding private chat
 * @param daily        the daily usage series, zero-filled
 * @param triggers     the trigger source ranking
 * @param groups       the top groups
 * @param topUsers     the top users
 */
public record AiUsageView(
        int days,
        long total,
        long inputTokens,
        long outputTokens,
        long activeUsers,
        long activeGroups,
        List<Day> daily,
        List<Rank> triggers,
        List<Rank> groups,
        List<Rank> topUsers
) {

    /**
     * 单日 AI 用量。
     * <p>AI usage of a single day.</p>
     *
     * @param date         the date
     * @param count        the AI turn count
     * @param inputTokens  the input token count
     * @param outputTokens the output token count
     */
    public record Day(LocalDate date, long count, long inputTokens, long outputTokens) {
    }

    /**
     * AI 用量排行项。
     * <p>AI usage ranking entry.</p>
     *
     * @param key          the trigger label, group id, or user id
     * @param name         the display name
     * @param count        the AI turn count
     * @param inputTokens  the input token count
     * @param outputTokens the output token count
     */
    public record Rank(String key, String name, long count, long inputTokens, long outputTokens) {
    }
}
