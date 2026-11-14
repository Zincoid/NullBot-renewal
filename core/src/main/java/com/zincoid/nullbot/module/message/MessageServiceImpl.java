package com.zincoid.nullbot.module.message;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.zincoid.nullbot.module.config.Conf;
import com.zincoid.nullbot.module.config.ConfigService;
import com.zincoid.nullbot.platform.persistence.mapper.MessageMapper;
import com.zincoid.nullbot.platform.persistence.po.Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * {@link MessageService} 的默认实现。
 * <p>Default implementation of {@link MessageService}.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageServiceImpl extends ServiceImpl<MessageMapper, Message> implements MessageService {

    private final ConfigService configService;
    private final AttachmentService attachmentService;

    // ════ Append ══════════════════

    @Override
    public Message append(Message message) {
        if (message == null)
            throw new MessageException("Missing key: message");
        if (message.getId() == null || message.getId().isBlank())
            throw new MessageException("Missing key: id");
        if (message.getChatId() == null || message.getChatId().isBlank())
            throw new MessageException("Missing key: chatId");
        if (message.getUserId() == null || message.getUserId().isBlank())
            throw new MessageException("Missing key: userId");
        if (message.getRole() == null)
            throw new MessageException("Missing key: role");
        if (message.getNickname() == null)
            throw new MessageException("Missing key: nickname");
        if (message.getContent() == null || message.getContent().isEmpty())
            throw new MessageException("Missing content");
        message.setReasoning(message.getReasoning() == null || message.getReasoning().isEmpty()
                ? null : message.getReasoning());
        message.setCreatedAt(LocalDateTime.now());
        save(message);
        log.info("Message saved: {}", message.getId());
        trimMessages(message.getChatId());
        return message;
    }

    // ════ Window ══════════════════

    @Override
    public int windowSize() {
        int size = configService.getInt(Conf.MESSAGE_HISTORY_MAX_LENGTH);
        if (size <= 0) throw new MessageException("Malformed value: " + size);
        return size;
    }

    @Override
    public List<Message> window(String chatId) {
        if (chatId == null || chatId.isBlank()) throw new MessageException("Missing key: chatId");
        List<Message> newest = lambdaQuery()
                .eq(Message::getChatId, chatId)
                .orderByDesc(Message::getCreatedAt)
                .orderByDesc(Message::getId)
                .last("LIMIT " + windowSize())
                .list();
        Collections.reverse(newest);
        return newest;
    }

    @Override
    public List<Message> overflow(String chatId, int window) {
        if (chatId == null || chatId.isBlank()) throw new MessageException("Missing key: chatId");
        if (window <= 0) throw new MessageException("Malformed value: " + window);
        List<Message> newest = lambdaQuery()
                .eq(Message::getChatId, chatId)
                .orderByDesc(Message::getCreatedAt)
                .orderByDesc(Message::getId)
                .last("LIMIT " + 2 * window)
                .list();
        Collections.reverse(newest);
        if (newest.size() <= window) return List.of();
        return List.copyOf(newest.subList(0, newest.size() - window));
    }

    // ════ Chats ═══════════════════

    @Override
    public List<ChatSummary> chats() {
        return baseMapper.chats().stream()
                .map(row -> new ChatSummary(
                        row.getChatId(),
                        row.getMessages() == null ? 0L : row.getMessages(),
                        row.getLastAt()))
                .toList();
    }

    // ════ Remove ══════════════════

    @Override
    public int remove(Collection<String> ids) {
        if (ids == null || ids.isEmpty()) return 0;
        List<String> distinct = ids.stream()
                .filter(id -> id != null && !id.isBlank())
                .distinct()
                .toList();
        if (distinct.isEmpty()) return 0;
        int removed = baseMapper.deleteByIds(distinct);
        distinct.forEach(this::deleteAttachments);
        log.info("Messages removed: {}", removed);
        return removed;
    }

    @Override
    public int clear(String chatId) {
        if (chatId == null || chatId.isBlank()) throw new MessageException("Missing key: chatId");
        List<Message> messages = lambdaQuery()
                .select(Message::getId)
                .eq(Message::getChatId, chatId)
                .list();
        if (messages.isEmpty()) return 0;
        removeByIds(messages.stream().map(Message::getId).toList());
        messages.forEach(message -> deleteAttachments(message.getId()));
        log.info("Chat history cleared: {}, messages={}", chatId, messages.size());
        return messages.size();
    }

    // ════ Helpers ═════════════════

    /**
     * 将会话窗口修剪至配置长度的两倍，为情景摘要留出溢出区。
     * <p>Trims the chat window to twice the configured size, leaving an overflow area for episodic summaries.</p>
     *
     * @param chatId chat identifier
     */
    private void trimMessages(String chatId) {
        int keep = 2 * windowSize();
        List<Message> all = lambdaQuery()
                .select(Message::getId)
                .eq(Message::getChatId, chatId)
                .orderByDesc(Message::getCreatedAt)
                .orderByDesc(Message::getId)
                .list();
        if (all.size() <= keep) return;
        List<Message> overflow = all.subList(keep, all.size());
        removeByIds(overflow.stream().map(Message::getId).toList());
        overflow.forEach(message -> deleteAttachments(message.getId()));
    }

    /**
     * 删除消息所拥有的聊天附件及其磁盘文件。
     * <p>Deletes the chat attachments owned by a message and their stored files.</p>
     *
     * @param messageId the removed message id
     */
    private void deleteAttachments(String messageId) {
        try {
            attachmentService.deleteByMessageId(messageId);
        } catch (RuntimeException e) {
            log.warn("Failed to delete attachments: message={}", messageId, e);
        }
    }
}
