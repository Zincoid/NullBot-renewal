package com.zincoid.nullbot.platform.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zincoid.nullbot.platform.persistence.po.Memory;
import com.zincoid.nullbot.platform.persistence.row.MemoryChatRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface MemoryMapper extends BaseMapper<Memory> {

    /**
     * 按会话汇总记忆数量与最近更新时间。
     * <p>Summarizes memory counts and the latest update time per chat.</p>
     *
     * @return the memory chat rows, newest first
     */
    @Select("""
            SELECT `chat_id` AS `chatId`,
                   COUNT(CASE WHEN `type` = 0 THEN 1 END) AS `episodic`,
                   COUNT(CASE WHEN `type` = 1 THEN 1 END) AS `semantic`,
                   MAX(COALESCE(`updated_at`, `created_at`)) AS `lastAt`
            FROM `memory`
            GROUP BY `chat_id`
            ORDER BY MAX(COALESCE(`updated_at`, `created_at`)) DESC, `chat_id` ASC
            """)
    List<MemoryChatRow> chats();
}
