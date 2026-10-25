package com.zincoid.nullbot.console.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * 控制台用量视图。
 * <p>Console view of usage statistics.</p>
 *
 * @param days         the number of days in the range
 * @param total        the total usage count
 * @param activeUsers  the distinct active users
 * @param activeGroups the distinct active groups, excluding private chat
 * @param daily        the daily usage series, zero-filled
 * @param cmds         the top commands
 * @param groups       the top groups
 * @param topUsers     the top users
 */
public record UsageView(
        int days,
        long total,
        long activeUsers,
        long activeGroups,
        List<Day> daily,
        List<Rank> cmds,
        List<Rank> groups,
        List<Rank> topUsers
) {

    /**
     * 单日用量。
     * <p>Usage of a single day.</p>
     *
     * @param date  the date
     * @param count the usage count
     */
    public record Day(LocalDate date, long count) {
    }

    /**
     * 用量排行项。
     * <p>Usage ranking entry.</p>
     *
     * @param key   the command name, group id, or user id
     * @param name  the display name
     * @param count the usage count
     */
    public record Rank(String key, String name, long count) {
    }
}
