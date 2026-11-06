package com.zincoid.nullbot.module.message;

import com.baomidou.mybatisplus.spring.service.IService;
import com.zincoid.nullbot.platform.persistence.po.Message;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 聊天消息记录服务：负责消息落库、会话窗口与历史清理。
 * <p>Chat message record service: message persistence, chat window, and history cleanup.</p>
 */
public interface MessageService extends IService<Message> {

    // ════ Append ══════════════════

    /**
     * 追加一条消息，并将会话窗口修剪至配置长度。
     * <p>Appends a message and trims the chat window to the configured size.</p>
     *
     * @param message the message to append
     * @return the persisted record
     */
    Message append(Message message);

    // ════ Window ══════════════════

    /**
     * 返回会话最近的消息，按时间正序。
     * <p>Returns the newest messages of the chat in chronological order.</p>
     *
     * @param chatId chat identifier
     * @return the chat window, oldest first
     */
    List<Message> window(String chatId);

    // ════ Chats ═══════════════════

    /**
     * 列出保留有消息的会话摘要，按最后消息时间倒序。
     * <p>Lists chat summaries with retained messages, newest activity first.</p>
     *
     * @return the chat summaries
     */
    List<ChatSummary> chats();

    /**
     * 会话摘要：会话标识、保留消息数与最后消息时间。
     * <p>Chat summary: chat identifier, retained message count, and last message time.</p>
     *
     * @param chatId   the composite chat id
     * @param messages the retained message count
     * @param lastAt   the last message time
     */
    record ChatSummary(String chatId, long messages, LocalDateTime lastAt) {
    }

    // ════ Clear ═══════════════════

    /**
     * 清空会话的全部消息，并删除其中的聊天附件。
     * <p>Clears all messages of the chat and deletes their chat attachments.</p>
     *
     * @param chatId chat identifier
     * @return the number of deleted messages
     */
    int clear(String chatId);
}
