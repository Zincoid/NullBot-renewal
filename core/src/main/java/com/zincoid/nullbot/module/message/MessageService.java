package com.zincoid.nullbot.module.message;

import com.baomidou.mybatisplus.spring.service.IService;
import com.zincoid.nullbot.platform.persistence.enums.MessageRole;
import com.zincoid.nullbot.platform.persistence.po.Message;

import java.util.List;

/**
 * 聊天消息记录服务：记录与删除。
 * <p>Chat message record service: record and delete.</p>
 */
public interface MessageService extends IService<Message> {

    // ════ Save ════════════════════

    /**
     * 保存一条聊天消息。
     * <p>Saves a chat message.</p>
     *
     * @param id       platform message id
     * @param chatId   chat identifier
     * @param userId   sender platform user id
     * @param role     message role
     * @param nickname  sender nickname
     * @param content   text content, may be blank when files present
     * @param reasoning assistant reasoning content, may be null
     * @param files     attached file paths, may be empty
     * @return the persisted record
     */
    Message save(String id, String chatId, String userId, MessageRole role, String nickname,
                 String content, String reasoning, List<String> files);

    // ════ Delete ══════════════════

    /**
     * 删除消息记录。
     * <p>Deletes the message record.</p>
     *
     * @param id platform message id
     */
    void delete(String id);
}
