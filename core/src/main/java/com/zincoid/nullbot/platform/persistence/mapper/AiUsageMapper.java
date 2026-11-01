package com.zincoid.nullbot.platform.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zincoid.nullbot.platform.persistence.enums.AiSource;
import com.zincoid.nullbot.platform.persistence.enums.ChatType;
import com.zincoid.nullbot.platform.persistence.po.AiUsage;
import com.zincoid.nullbot.platform.persistence.row.AiUsageDailyRow;
import com.zincoid.nullbot.platform.persistence.row.AiUsageRankRow;
import com.zincoid.nullbot.platform.persistence.row.AiUsageSummaryRow;
import com.zincoid.nullbot.platform.persistence.row.AiUsageTriggerRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

/**
 * AI 使用记录 Mapper。
 * <p>AI usage record mapper.</p>
 */
@Mapper
public interface AiUsageMapper extends BaseMapper<AiUsage> {

    /**
     * 增加一次 AI 回合的计数与 token，同一天同会话同用户同触发来源累加。
     * <p>Increases the AI turn count and tokens for the same day, chat, user, and trigger.</p>
     *
     * @param time         the statistics date
     * @param chatType     the chat type
     * @param chatId       raw group or user platform id
     * @param userId       triggering platform user id
     * @param source       the trigger source
     * @param inputTokens  the input token count, nullable
     * @param outputTokens the output token count, nullable
     */
    void increase(@Param("time") LocalDate time,
                  @Param("chatType") ChatType chatType,
                  @Param("chatId") String chatId,
                  @Param("userId") String userId,
                  @Param("source") AiSource source,
                  @Param("inputTokens") Integer inputTokens,
                  @Param("outputTokens") Integer outputTokens);

    /**
     * 汇总指定日期范围内的 AI 对话、token 与活跃会话。
     * <p>Summarizes AI turns, tokens, and active chats in the date range.</p>
     *
     * @param start inclusive start date
     * @param end   inclusive end date
     * @return the summary row
     */
    @Select("""
            SELECT COALESCE(SUM(`count`), 0) AS `total`,
                   COALESCE(SUM(`input_tokens`), 0) AS `inputTokens`,
                   COALESCE(SUM(`output_tokens`), 0) AS `outputTokens`,
                   COUNT(DISTINCT `user_id`) AS `users`,
                   COUNT(DISTINCT CASE WHEN `chat_type` = 0 THEN `chat_id` END) AS `groups`
            FROM `ai_usage`
            WHERE `time` BETWEEN #{start} AND #{end}
            """)
    AiUsageSummaryRow summary(@Param("start") LocalDate start, @Param("end") LocalDate end);

    /**
     * 汇总全量 AI 用量。
     * <p>Summarizes the all-time AI usage.</p>
     *
     * @return the all-time summary row
     */
    @Select("""
            SELECT COALESCE(SUM(`count`), 0) AS `total`,
                   COALESCE(SUM(`input_tokens`), 0) AS `inputTokens`,
                   COALESCE(SUM(`output_tokens`), 0) AS `outputTokens`
            FROM `ai_usage`
            """)
    AiUsageSummaryRow total();

    /**
     * 按天汇总指定日期范围内的 AI 对话与 token。
     * <p>Summarizes AI turns and tokens per day in the date range.</p>
     *
     * @param start inclusive start date
     * @param end   inclusive end date
     * @return the daily rows, oldest first
     */
    @Select("""
            SELECT `time` AS `day`,
                   COALESCE(SUM(`count`), 0) AS `count`,
                   COALESCE(SUM(`input_tokens`), 0) AS `inputTokens`,
                   COALESCE(SUM(`output_tokens`), 0) AS `outputTokens`
            FROM `ai_usage`
            WHERE `time` BETWEEN #{start} AND #{end}
            GROUP BY `time`
            ORDER BY `time`
            """)
    List<AiUsageDailyRow> daily(@Param("start") LocalDate start, @Param("end") LocalDate end);

    /**
     * 汇总指定日期范围内的触发来源。
     * <p>Summarizes trigger sources in the date range.</p>
     *
     * @param start inclusive start date
     * @param end   inclusive end date
     * @return the trigger rows
     */
    @Select("""
            SELECT `source`,
                   COALESCE(SUM(`count`), 0) AS `count`,
                   COALESCE(SUM(`input_tokens`), 0) AS `inputTokens`,
                   COALESCE(SUM(`output_tokens`), 0) AS `outputTokens`
            FROM `ai_usage`
            WHERE `time` BETWEEN #{start} AND #{end}
            GROUP BY `source`
            ORDER BY `count` DESC, `source`
            """)
    List<AiUsageTriggerRow> triggers(@Param("start") LocalDate start, @Param("end") LocalDate end);

    /**
     * 汇总指定日期范围内使用最多的群。
     * <p>Summarizes the most active groups in the date range.</p>
     *
     * @param start inclusive start date
     * @param end   inclusive end date
     * @param limit maximum number of rows
     * @return the group ranking rows
     */
    @Select("""
            SELECT `chat_id` AS `key`,
                   COALESCE(SUM(`count`), 0) AS `count`,
                   COALESCE(SUM(`input_tokens`), 0) AS `inputTokens`,
                   COALESCE(SUM(`output_tokens`), 0) AS `outputTokens`
            FROM `ai_usage`
            WHERE `chat_type` = 0
              AND `time` BETWEEN #{start} AND #{end}
            GROUP BY `chat_id`
            ORDER BY `count` DESC, `key`
            LIMIT #{limit}
            """)
    List<AiUsageRankRow> groups(@Param("start") LocalDate start,
                                @Param("end") LocalDate end,
                                @Param("limit") int limit);

    /**
     * 汇总指定日期范围内使用最多的用户。
     * <p>Summarizes the most active users in the date range.</p>
     *
     * @param start inclusive start date
     * @param end   inclusive end date
     * @param limit maximum number of rows
     * @return the user ranking rows
     */
    @Select("""
            SELECT `user_id` AS `key`,
                   COALESCE(SUM(`count`), 0) AS `count`,
                   COALESCE(SUM(`input_tokens`), 0) AS `inputTokens`,
                   COALESCE(SUM(`output_tokens`), 0) AS `outputTokens`
            FROM `ai_usage`
            WHERE `time` BETWEEN #{start} AND #{end}
            GROUP BY `user_id`
            ORDER BY `count` DESC, `key`
            LIMIT #{limit}
            """)
    List<AiUsageRankRow> users(@Param("start") LocalDate start,
                               @Param("end") LocalDate end,
                               @Param("limit") int limit);
}
