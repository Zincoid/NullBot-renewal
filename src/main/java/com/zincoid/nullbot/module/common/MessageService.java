package com.zincoid.nullbot.module.common;

import com.baomidou.mybatisplus.spring.service.IService;
import com.zincoid.nullbot.platform.persistence.po.Message;

import java.util.List;

/**
 * Chat message record service: record and delete.
 * <p>聊天消息记录服务：记录与删除。</p>
 */
public interface MessageService extends IService<Message> {

    // ════ Save ════════════════════

    /**
     * Saves a chat message.
     * <p>保存一条聊天消息。</p>
     *
     * @param id      platform message ID
     * @param chatId  chat identifier
     * @param userId  sender platform user ID
     * @param content text content, may be blank when files present
     * @param files   attached file paths, may be empty
     * @return the persisted record
     */
    Message save(String id, String chatId, String userId, String content, List<String> files);

    // ════ Delete ══════════════════

    /**
     * Deletes the message record.
     * <p>删除消息记录。</p>
     *
     * @param id platform message ID
     */
    void delete(String id);
}
