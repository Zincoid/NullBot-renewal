package com.zincoid.nullbot.module.system;

import com.baomidou.mybatisplus.spring.service.IService;
import com.zincoid.nullbot.platform.persistence.po.Usage;

import java.time.LocalDate;
import java.util.List;

/**
 * 指令用量服务：按天累积使用次数。
 * <p>Command usage service: accumulates daily usage counts.</p>
 */
public interface UsageService extends IService<Usage> {

    // ════ Increase ════════════════

    /**
     * 增加一次指令使用计数。
     * <p>Increases the command usage count by one.</p>
     *
     * @param groupId group platform id, {@code 0} for private chat
     * @param userId  operator platform user id
     * @param cmd     command name
     */
    void increase(String groupId, String userId, String cmd);

    // ════ Stats ═══════════════════

    /**
     * 汇总指定日期范围内的用量。
     * <p>Summarizes usage in the date range.</p>
     *
     * @param start inclusive start date
     * @param end   inclusive end date
     * @param limit maximum size of each ranking
     * @return the usage statistics
     */
    Stats stats(LocalDate start, LocalDate end, int limit);

    /**
     * 单日用量。
     * <p>Usage of a single day.</p>
     *
     * @param day   the date
     * @param count the usage count
     */
    record Daily(LocalDate day, long count) {
    }

    /**
     * 用量排行项。
     * <p>Usage ranking entry.</p>
     *
     * @param key   the command name, group id, or user id
     * @param count the usage count
     */
    record Rank(String key, long count) {
    }

    /**
     * 用量统计结果。
     * <p>Usage statistics result.</p>
     *
     * @param total     total usage count
     * @param users     distinct active users
     * @param groups    distinct active groups, excluding private chat
     * @param daily     daily usage rows with data
     * @param cmds      top commands
     * @param topGroups top groups
     * @param topUsers  top users
     */
    record Stats(long total, long users, long groups, List<Daily> daily,
                 List<Rank> cmds, List<Rank> topGroups, List<Rank> topUsers) {
    }
}
