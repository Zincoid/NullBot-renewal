package com.zincoid.nullbot.platform.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zincoid.nullbot.platform.persistence.enums.ChatType;
import com.zincoid.nullbot.platform.persistence.po.CmdUsage;
import com.zincoid.nullbot.platform.persistence.row.CmdUsageDailyRow;
import com.zincoid.nullbot.platform.persistence.row.CmdUsageRankRow;
import com.zincoid.nullbot.platform.persistence.row.CmdUsageSummaryRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface CmdUsageMapper extends BaseMapper<CmdUsage> {

    /**
     * 增加一条使用计数，同日同会话同用户同指令累加。
     * <p>Increases the usage count for the same day, chat, user, and command.</p>
     *
     * @param time     the statistics date
     * @param chatType the chat type
     * @param targetId raw group or user platform id
     * @param userId   the operator platform user id
     * @param cmd      the command name
     */
    void increase(@Param("time") LocalDate time,
                  @Param("chatType") ChatType chatType,
                  @Param("targetId") String targetId,
                  @Param("userId") String userId,
                  @Param("cmd") String cmd);

    /**
     * 统计指定用户的历史总用量。
     * <p>Counts the all-time total usage of the given user.</p>
     *
     * @param userId the platform user id
     * @return the total usage count
     */
    @Select("""
            SELECT COALESCE(SUM(`count`), 0)
            FROM `cmd_usage`
            WHERE `user_id` = #{userId}
            """)
    long totalByUser(@Param("userId") String userId);

    /**
     * 统计全量指令用量。
     * <p>Counts the all-time total usage.</p>
     *
     * @return the total usage count
     */
    @Select("""
            SELECT COALESCE(SUM(`count`), 0)
            FROM `cmd_usage`
            """)
    long total();

    /**
     * 汇总指定日期范围内的用量、活跃用户与活跃群。
     * <p>Summarizes usage, active users, and active groups in the date range.</p>
     *
     * @param start inclusive start date
     * @param end   inclusive end date
     * @return the summary row
     */
    @Select("""
            SELECT COALESCE(SUM(`count`), 0) AS `total`,
                   COUNT(DISTINCT `user_id`) AS `users`,
                   COUNT(DISTINCT CASE WHEN `chat_type` = 0 THEN `target_id` END) AS `groups`
            FROM `cmd_usage`
            WHERE `time` BETWEEN #{start} AND #{end}
            """)
    CmdUsageSummaryRow summary(@Param("start") LocalDate start, @Param("end") LocalDate end);

    /**
     * 按天汇总指定日期范围内的用量。
     * <p>Summarizes usage per day in the date range.</p>
     *
     * @param start inclusive start date
     * @param end   inclusive end date
     * @return the daily rows, oldest first
     */
    @Select("""
            SELECT `time` AS `day`, COALESCE(SUM(`count`), 0) AS `count`
            FROM `cmd_usage`
            WHERE `time` BETWEEN #{start} AND #{end}
            GROUP BY `time`
            ORDER BY `time`
            """)
    List<CmdUsageDailyRow> daily(@Param("start") LocalDate start, @Param("end") LocalDate end);

    /**
     * 汇总指定日期范围内使用最多的指令。
     * <p>Summarizes the most used commands in the date range.</p>
     *
     * @param start inclusive start date
     * @param end   inclusive end date
     * @param limit maximum number of rows
     * @return the command ranking rows
     */
    @Select("""
            SELECT `cmd` AS `name`, COALESCE(SUM(`count`), 0) AS `count`
            FROM `cmd_usage`
            WHERE `time` BETWEEN #{start} AND #{end}
            GROUP BY `cmd`
            ORDER BY `count` DESC, `name`
            LIMIT #{limit}
            """)
    List<CmdUsageRankRow> cmds(@Param("start") LocalDate start,
                            @Param("end") LocalDate end,
                            @Param("limit") int limit);

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
            SELECT `target_id` AS `name`, COALESCE(SUM(`count`), 0) AS `count`
            FROM `cmd_usage`
            WHERE `chat_type` = 0
              AND `time` BETWEEN #{start} AND #{end}
            GROUP BY `target_id`
            ORDER BY `count` DESC, `name`
            LIMIT #{limit}
            """)
    List<CmdUsageRankRow> groups(@Param("start") LocalDate start,
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
            SELECT `user_id` AS `name`, COALESCE(SUM(`count`), 0) AS `count`
            FROM `cmd_usage`
            WHERE `time` BETWEEN #{start} AND #{end}
            GROUP BY `user_id`
            ORDER BY `count` DESC, `name`
            LIMIT #{limit}
            """)
    List<CmdUsageRankRow> users(@Param("start") LocalDate start,
                             @Param("end") LocalDate end,
                             @Param("limit") int limit);
}
