package com.zincoid.nullbot.bot.memory;

import com.zincoid.nullbot.module.common.MessageService;
import com.zincoid.nullbot.module.config.Conf;
import com.zincoid.nullbot.module.config.ConfigService;
import com.zincoid.nullbot.module.file.FileService;
import com.zincoid.nullbot.platform.persistence.enums.MessageRole;
import com.zincoid.nullbot.platform.persistence.po.Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * 按会话划分的消息窗口：每个会话仅保留最近 N 条消息记录，写入时修剪。
 * <p>Per-chat message window over the stored message records: each chat retains the newest N messages, trimmed on write.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MsgWindowStore {

    private final MessageService messageService;
    private final ConfigService configService;
    private final FileService fileService;

    // ════ Add ═════════════════════

    /**
     * 记录一条消息，并将会话窗口修剪至配置长度。
     * <p>Records a message and trims the chat window to the configured size.</p>
     *
     * @param chatId    chat identifier id
     * @param messageId platform message id
     * @param userId    sender platform user id
     * @param role      message role
     * @param nickname  sender nickname
     * @param content   text content, may be blank when files present
     * @param reasoning assistant reasoning content, may be null
     * @param files     attached file paths, may be empty
     * @return the persisted record
     */
    public Message add(String chatId, String messageId,
                       String userId, MessageRole role, String nickname,
                       String content, String reasoning, List<String> files) {
        Message message = messageService.save(
                messageId, chatId,
                userId, role, nickname,
                content, reasoning, files
        );
        trim(chatId);
        return message;
    }

    // ════ Get ═════════════════════

    /**
     * 返回该会话最近的消息，按时间正序。
     * <p>Returns the newest messages of the chat in chronological order.</p>
     *
     * @param chatId chat identifier
     * @return the chat window, oldest first
     */
    public List<Message> get(String chatId) {
        if (chatId == null || chatId.isBlank()) throw new MemoryException("Missing key: chatId");
        List<Message> newest = messageService.lambdaQuery()
                .eq(Message::getChatId, chatId)
                .orderByDesc(Message::getCreatedAt)
                .last("LIMIT " + size())
                .list();
        Collections.reverse(newest);
        return newest;
    }

    // ════ Helpers ═════════════════

    /**
     * 读取配置的窗口长度。
     * <p>Reads the configured window size.</p>
     *
     * @return the window size
     */
    private int size() {
        int size = configService.getInt(Conf.MESSAGE_HISTORY_MAX_LENGTH);
        if (size <= 0) throw new MemoryException("Malformed value: " + size);
        return size;
    }

    /**
     * 将会话窗口修剪至配置长度。
     * <p>Trims the chat window to the configured size.</p>
     *
     * @param chatId chat identifier
     */
    private void trim(String chatId) {
        int size = size();
        List<Message> all = messageService.lambdaQuery()
                .eq(Message::getChatId, chatId)
                .orderByDesc(Message::getCreatedAt)
                .list();
        if (all.size() <= size) return;
        List<Message> overflow = all.subList(size, all.size());
        messageService.removeByIds(overflow.stream().map(Message::getId).toList());
        overflow.forEach(this::deleteAttachments);
    }

    /**
     * 删除被修剪消息所引用的附件。
     * <p>Deletes the attachments referenced by a trimmed message.</p>
     *
     * @param message the trimmed message
     */
    private void deleteAttachments(Message message) {
        if (message.getFiles() == null) return;
        for (String fileId : message.getFiles()) {
            try {
                fileService.delete(Long.valueOf(fileId));
            } catch (RuntimeException e) {
                log.warn("Failed to delete attachment: {}", fileId, e);
            }
        }
    }
}
