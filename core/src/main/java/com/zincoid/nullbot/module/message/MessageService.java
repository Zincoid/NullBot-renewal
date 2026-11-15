package com.zincoid.nullbot.module.message;

import com.baomidou.mybatisplus.spring.service.IService;
import com.zincoid.nullbot.platform.persistence.po.Message;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * 聊天消息记录服务：负责消息落库、读取状态、会话窗口与历史清理。
 * <p>Chat message record service: message persistence, read status, chat window, and history cleanup.</p>
 */
public interface MessageService extends IService<Message> {

    // ════ Append ══════════════════

    /**
     * 追加一条消息，并将会话窗口修剪至配置长度，未压缩的已读消息会保留。
     * <p>Appends a message and trims the chat window to the configured size, retaining uncompacted read messages.</p>
     *
     * @param message the message to append
     * @return the persisted record
     */
    Message append(Message message);

    // ════ Window ══════════════════

    /**
     * 读取配置的会话窗口长度。
     * <p>Reads the configured window size.</p>
     *
     * @return the window size
     */
    int windowSize();

    /**
     * 返回会话最近的消息，按时间正序。
     * <p>Returns the newest messages of the chat in chronological order.</p>
     *
     * @param chatId chat identifier
     * @return the chat window, oldest first
     */
    List<Message> window(String chatId);

    // ════ Status ══════════════════

    /**
     * 返回会话中等待压缩的已读消息，按时间正序。
     * <p>Returns the read messages awaiting compaction in chronological order.</p>
     *
     * @param chatId chat identifier
     * @return the read messages, oldest first
     */
    List<Message> getRead(String chatId);

    /**
     * 返回会话中尚未压缩的消息，按时间正序。
     * <p>Returns the uncompacted messages of the chat in chronological order.</p>
     *
     * @param chatId chat identifier
     * @return the uncompacted messages, oldest first
     */
    List<Message> getUncompacted(String chatId);

    /**
     * 将未读消息标记为已读，已压缩的消息不变。
     * <p>Marks unread messages as read; compacted messages are unchanged.</p>
     *
     * @param ids the message ids
     */
    void markRead(Collection<String> ids);

    /**
     * 将已读消息标记为已压缩，未读的消息不变。
     * <p>Marks read messages as compacted; unread messages are unchanged.</p>
     *
     * @param ids the message ids
     */
    void markCompacted(Collection<String> ids);

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

    // ════ Remove ══════════════════

    /**
     * 按 id 删除指定消息及其聊天附件。
     * <p>Deletes the given messages and their chat attachments by id.</p>
     *
     * @param ids the message ids
     * @return the number of deleted messages
     */
    int remove(Collection<String> ids);

    /**
     * 清空会话的全部消息，并删除其中的聊天附件。
     * <p>Clears all messages of the chat and deletes their chat attachments.</p>
     *
     * @param chatId chat identifier
     * @return the number of deleted messages
     */
    int clear(String chatId);
}
