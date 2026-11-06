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
        trim(message.getChatId());
        return message;
    }

    // ════ Window ══════════════════

    @Override
    public List<Message> window(String chatId) {
        if (chatId == null || chatId.isBlank()) throw new MessageException("Missing key: chatId");
        List<Message> newest = lambdaQuery()
                .eq(Message::getChatId, chatId)
                .orderByDesc(Message::getCreatedAt)
                .last("LIMIT " + size())
                .list();
        Collections.reverse(newest);
        return newest;
    }

    // ════ Clear ═══════════════════

    @Override
    public int clear(String chatId) {
        if (chatId == null || chatId.isBlank()) throw new MessageException("Missing key: chatId");
        List<Message> messages = lambdaQuery()
                .select(Message::getId)
                .eq(Message::getChatId, chatId)
                .list();
        if (messages.isEmpty()) return 0;
        removeByIds(messages.stream().map(Message::getId).toList());
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
        List<Message> all = lambdaQuery()
                .select(Message::getId)
                .eq(Message::getChatId, chatId)
                .orderByDesc(Message::getCreatedAt)
                .list();
        if (all.size() <= size) return;
        List<Message> overflow = all.subList(size, all.size());
        removeByIds(overflow.stream().map(Message::getId).toList());
        overflow.forEach(this::deleteAttachments);
    }

    /**
     * 删除消息所拥有的聊天附件及其磁盘文件。
     * <p>Deletes the chat attachments owned by a message and their stored files.</p>
     *
     * @param message the removed message
     */
    private void deleteAttachments(Message message) {
        try {
            attachmentService.deleteByMessageId(message.getId());
        } catch (RuntimeException e) {
            log.warn("Failed to delete attachments: message={}", message.getId(), e);
        }
    }
}
