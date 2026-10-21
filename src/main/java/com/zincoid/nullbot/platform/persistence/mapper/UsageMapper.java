package com.zincoid.nullbot.platform.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zincoid.nullbot.platform.persistence.po.Usage;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;

@Mapper
public interface UsageMapper extends BaseMapper<Usage> {

    /**
     * 增加一条使用计数，同日同会话同用户同指令累加。
     * <p>Increases the usage count for the same day, chat, user, and command.</p>
     *
     * @param time    the statistics date
     * @param groupId the group platform ID, {@code 0} for private chat
     * @param userId  the operator platform user ID
     * @param cmd     the command name
     */
    @Insert("INSERT INTO `usage` (`time`, `group_id`, `user_id`, `cmd`, `count`) " +
            "VALUES (#{time}, #{groupId}, #{userId}, #{cmd}, 1) " +
            "ON DUPLICATE KEY UPDATE `count` = `count` + 1")
    void increase(@Param("time") LocalDate time,
                  @Param("groupId") String groupId,
                  @Param("userId") String userId,
                  @Param("cmd") String cmd);
}
