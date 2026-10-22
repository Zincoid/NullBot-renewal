package com.zincoid.nullbot.platform.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zincoid.nullbot.platform.persistence.po.Usage;
import com.zincoid.nullbot.platform.persistence.row.UsageDailyRow;
import com.zincoid.nullbot.platform.persistence.row.UsageRankRow;
import com.zincoid.nullbot.platform.persistence.row.UsageSummaryRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface UsageMapper extends BaseMapper<Usage> {

    /**
     * 增加一条使用计数，同日同会话同用户同指令累加。
     * <p>Increases the usage count for the same day, chat, user, and command.</p>
     *
     * @param time    the statistics date
     * @param groupId the group platform id, {@code 0} for private chat
     * @param userId  the operator platform user id
     * @param cmd     the command name
     */
    void increase(@Param("time") LocalDate time,
                  @Param("groupId") String groupId,
                  @Param("userId") String userId,
                  @Param("cmd") String cmd);

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
                   COUNT(DISTINCT CASE WHEN `group_id` <> '0' THEN `group_id` END) AS `groups`
            FROM `usage`
            WHERE `time` BETWEEN #{start} AND #{end}
            """)
    UsageSummaryRow summary(@Param("start") LocalDate start, @Param("end") LocalDate end);

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
            FROM `usage`
            WHERE `time` BETWEEN #{start} AND #{end}
            GROUP BY `time`
            ORDER BY `time`
            """)
    List<UsageDailyRow> daily(@Param("start") LocalDate start, @Param("end") LocalDate end);

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
            FROM `usage`
            WHERE `time` BETWEEN #{start} AND #{end}
            GROUP BY `cmd`
            ORDER BY `count` DESC
            LIMIT #{limit}
            """)
    List<UsageRankRow> commands(@Param("start") LocalDate start,
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
            SELECT `group_id` AS `name`, COALESCE(SUM(`count`), 0) AS `count`
            FROM `usage`
            WHERE `time` BETWEEN #{start} AND #{end}
            GROUP BY `group_id`
            ORDER BY `count` DESC
            LIMIT #{limit}
            """)
    List<UsageRankRow> groups(@Param("start") LocalDate start,
                              @Param("end") LocalDate end,
                              @Param("limit") int limit);
}
