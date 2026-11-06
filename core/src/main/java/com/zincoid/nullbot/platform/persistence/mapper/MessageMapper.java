package com.zincoid.nullbot.platform.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zincoid.nullbot.platform.persistence.po.Message;
import com.zincoid.nullbot.platform.persistence.row.MessageChatRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface MessageMapper extends BaseMapper<Message> {

    /**
     * 按会话汇总保留的消息数与最后消息时间。
     * <p>Summarizes retained message counts and last message times per chat.</p>
     *
     * @return the chat summary rows, newest activity first
     */
    @Select("""
            SELECT `chat_id` AS `chatId`,
                   COUNT(*) AS `messages`,
                   MAX(`created_at`) AS `lastAt`
            FROM `message`
            GROUP BY `chat_id`
            ORDER BY MAX(`created_at`) DESC, `chat_id` ASC
            """)
    List<MessageChatRow> chats();
}
