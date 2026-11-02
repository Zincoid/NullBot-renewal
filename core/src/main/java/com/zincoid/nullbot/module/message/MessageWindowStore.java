package com.zincoid.nullbot.module.message;

import com.zincoid.nullbot.module.config.Conf;
import com.zincoid.nullbot.module.config.ConfigService;
import com.zincoid.nullbot.module.file.FileService;
import com.zincoid.nullbot.platform.persistence.enums.BizType;
import com.zincoid.nullbot.platform.persistence.enums.MessageRole;
import com.zincoid.nullbot.platform.persistence.po.File;
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
public class MessageWindowStore {

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
        if (chatId == null || chatId.isBlank()) throw new MessageException("Missing key: chatId");
        List<Message> newest = messageService.lambdaQuery()
                .eq(Message::getChatId, chatId)
                .orderByDesc(Message::getCreatedAt)
                .last("LIMIT " + size())
                .list();
        Collections.reverse(newest);
        return newest;
    }

    // ════ Clear ═══════════════════

    /**
     * 清空会话的全部消息，并删除其独占的聊天附件。
     * <p>Clears all messages of the chat and deletes the chat attachments they exclusively own.</p>
     *
     * @param chatId chat identifier
     * @return the number of deleted messages
     */
    public int clear(String chatId) {
        if (chatId == null || chatId.isBlank()) throw new MessageException("Missing key: chatId");
        List<Message> messages = messageService.lambdaQuery()
                .select(Message::getId, Message::getFiles)
                .eq(Message::getChatId, chatId)
                .list();
        if (messages.isEmpty()) return 0;
        messageService.removeByIds(messages.stream().map(Message::getId).toList());
        messages.forEach(this::deleteAttachments);
        log.info("Chat history cleared: {}, messages={}", chatId, messages.size());
        return messages.size();
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
        if (size <= 0) throw new MessageException("Malformed value: " + size);
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
                .select(Message::getId, Message::getFiles)
                .eq(Message::getChatId, chatId)
                .orderByDesc(Message::getCreatedAt)
                .list();
        if (all.size() <= size) return;
        List<Message> overflow = all.subList(size, all.size());
        messageService.removeByIds(overflow.stream().map(Message::getId).toList());
        overflow.forEach(this::deleteAttachments);
    }

    /**
     * 删除被修剪消息所引用的附件：仅删除会话消息自己保存的聊天文件，且不再被其他消息引用时才删除。
     * <p>Deletes the attachments of a trimmed message: only chat files saved for message records, and only when no other message still references them.</p>
     *
     * @param message the trimmed message
     */
    private void deleteAttachments(Message message) {
        if (message.getFiles() == null || message.getFiles().isEmpty()) return;
        for (String fileId : message.getFiles()) {
            try {
                File file = fileService.getById(Long.valueOf(fileId));
                if (file == null || file.getType() != BizType.CHAT) continue;
                if (isReferenced(message, fileId)) continue;
                fileService.delete(file.getId());
            } catch (RuntimeException e) {
                log.warn("Failed to delete attachment: {}", fileId, e);
            }
        }
    }

    /**
     * 判断同一会话中是否还有其他消息引用该附件。
     * <p>Checks whether another message in the same chat still references the attachment.</p>
     *
     * @param message the trimmed message
     * @param fileId  the attachment file record id
     * @return true when another message references the attachment
     */
    private boolean isReferenced(Message message, String fileId) {
        List<Message> messages = messageService.lambdaQuery()
                .select(Message::getFiles)
                .eq(Message::getChatId, message.getChatId())
                .ne(Message::getId, message.getId())
                .isNotNull(Message::getFiles)
                .list();
        for (Message other : messages) {
            if (other.getFiles() != null && other.getFiles().contains(fileId))
                return true;
        }
        return false;
    }
}
