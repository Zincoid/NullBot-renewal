package com.zincoid.nullbot.module.system;

import com.baomidou.mybatisplus.spring.service.IService;
import com.zincoid.nullbot.platform.persistence.enums.AiSource;
import com.zincoid.nullbot.platform.persistence.enums.ChatType;
import com.zincoid.nullbot.platform.persistence.po.AiUsage;

import java.time.LocalDate;
import java.util.List;

/**
 * AI 使用记录服务：按天累加 AI 对话回合的调用次数与 token。
 * <p>AI usage service: accumulates daily AI turn counts and tokens.</p>
 */
public interface AiUsageService extends IService<AiUsage> {

    /**
     * 将一次 AI 对话回合累加到当天的统计。
     * <p>Adds one AI conversation turn to the current day's statistics.</p>
     *
     * @param chatType     chat type
     * @param chatId       raw group or user platform id
     * @param userId       triggering platform user id
     * @param source       trigger source
     * @param inputTokens  input token count, nullable
     * @param outputTokens output token count, nullable
     */
    void increase(ChatType chatType, String chatId, String userId, AiSource source,
                  Integer inputTokens, Integer outputTokens);

    /**
     * 统计全量 AI 用量。
     * <p>Counts the all-time AI usage.</p>
     *
     * @return the all-time AI usage
     */
    Total total();

    /**
     * 汇总指定日期范围内的 AI 用量。
     * <p>Summarizes AI usage in the date range.</p>
     *
     * @param start inclusive start date
     * @param end   inclusive end date
     * @param limit maximum size of each ranking
     * @return the AI usage statistics
     */
    Stats stats(LocalDate start, LocalDate end, int limit);

    /**
     * 单日 AI 用量。
     * <p>AI usage of a single day.</p>
     *
     * @param day          the date
     * @param count        the AI turn count
     * @param inputTokens  the input token count
     * @param outputTokens the output token count
     */
    record Daily(LocalDate day, long count, long inputTokens, long outputTokens) {
    }

    /**
     * AI 用量排行项。
     * <p>AI usage ranking entry.</p>
     *
     * @param key          the trigger label, group id, or user id
     * @param count        the AI turn count
     * @param inputTokens  the input token count
     * @param outputTokens the output token count
     */
    record Rank(String key, long count, long inputTokens, long outputTokens) {
    }

    /**
     * AI 用量统计结果。
     * <p>AI usage statistics result.</p>
     *
     * @param total        total AI turn count
     * @param inputTokens  total input token count
     * @param outputTokens total output token count
     * @param users        distinct active users
     * @param groups       distinct active groups, excluding private chat
     * @param daily        daily usage rows with data
     * @param triggers     trigger source ranking
     * @param topGroups    top groups
     * @param topUsers     top users
     */
    record Stats(long total, long inputTokens, long outputTokens, long users, long groups,
                 List<Daily> daily, List<Rank> triggers,
                 List<Rank> topGroups, List<Rank> topUsers) {
    }

    /**
     * 全量 AI 用量。
     * <p>All-time AI usage.</p>
     *
     * @param count        the total AI turn count
     * @param inputTokens  the total input token count
     * @param outputTokens the total output token count
     */
    record Total(long count, long inputTokens, long outputTokens) {
    }
}
