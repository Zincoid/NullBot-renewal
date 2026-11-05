package com.zincoid.nullbot.module.message;

import com.baomidou.mybatisplus.spring.service.IService;
import com.zincoid.nullbot.platform.persistence.po.Message;

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

    // ════ Clear ═══════════════════

    /**
     * 清空会话的全部消息，并删除其独占的聊天附件。
     * <p>Clears all messages of the chat and deletes the chat attachments they exclusively own.</p>
     *
     * @param chatId chat identifier
     * @return the number of deleted messages
     */
    int clear(String chatId);
}
