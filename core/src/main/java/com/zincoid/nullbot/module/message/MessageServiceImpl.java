package com.zincoid.nullbot.module.message;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.zincoid.nullbot.module.config.Conf;
import com.zincoid.nullbot.module.config.ConfigService;
import com.zincoid.nullbot.module.memory.MemoryService;
import com.zincoid.nullbot.platform.persistence.enums.MessageStatus;
import com.zincoid.nullbot.platform.persistence.mapper.MessageMapper;
import com.zincoid.nullbot.platform.persistence.po.Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * {@link MessageService} 的默认实现。
 * <p>Default implementation of {@link MessageService}.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageServiceImpl extends ServiceImpl<MessageMapper, Message> implements MessageService {

    private static final int LOCK_STRIPES = 64;

    private final ReentrantLock[] locks = initLocks();

    private final ConfigService configService;
    private final AttachmentService attachmentService;
    private final ApplicationEventPublisher eventPublisher;
    private final MemoryService memoryService;

    // ════ Locking ═════════════════

    @Override
    public void runLocked(String chatId, Runnable action) {
        ReentrantLock lock = lock(chatId);
        lock.lock();
        try {
            action.run();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public <T> T callLocked(String chatId, Supplier<T> action) {
        ReentrantLock lock = lock(chatId);
        lock.lock();
        try {
            return action.get();
        } finally {
            lock.unlock();
        }
    }

    // ════ Record ══════════════════

    @Override
    public Message record(Message message) {
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
        if (message.getStatus() == null)
            message.setStatus(MessageStatus.UNREAD);
        message.setReasoning(message.getReasoning() == null || message.getReasoning().isEmpty()
                ? null : message.getReasoning());
        String chatId = message.getChatId();
        boolean blocked;
        ReentrantLock lock = lock(chatId);
        lock.lock();
        try {
            message.setCreatedAt(LocalDateTime.now());
            save(message);
            log.debug("Message saved: {}", message.getId());
            blocked = trimMessages(chatId);
        } finally {
            lock.unlock();
        }
        if (blocked) {
            try {
                eventPublisher.publishEvent(new MessageTrimBlockedEvent(chatId));
            } catch (RuntimeException e) {
                log.warn("Failed to publish trim blocked event: {}", e.getMessage());
            }
        }
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
        requireChatId(chatId);
        List<Message> newest = lambdaQuery()
                .eq(Message::getChatId, chatId)
                .orderByDesc(Message::getCreatedAt)
                .orderByDesc(Message::getId)
                .last("LIMIT " + windowSize())
                .list();
        Collections.reverse(newest);
        return newest;
    }

    // ════ Status ══════════════════

    @Override
    public List<Message> getRead(String chatId) {
        requireChatId(chatId);
        return lambdaQuery()
                .eq(Message::getChatId, chatId)
                .eq(Message::getStatus, MessageStatus.READ)
                .orderByAsc(Message::getCreatedAt)
                .orderByAsc(Message::getId)
                .list();
    }

    @Override
    public List<Message> getUncompacted(String chatId) {
        requireChatId(chatId);
        return lambdaQuery()
                .eq(Message::getChatId, chatId)
                .ne(Message::getStatus, MessageStatus.COMPACTED)
                .orderByAsc(Message::getCreatedAt)
                .orderByAsc(Message::getId)
                .list();
    }

    @Override
    public void markRead(Collection<String> ids) {
        updateStatus(ids, MessageStatus.UNREAD, MessageStatus.READ);
    }

    @Override
    public void markCompacted(Collection<String> ids) {
        updateStatus(ids, MessageStatus.READ, MessageStatus.COMPACTED);
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
        log.debug("Messages removed: {}", removed);
        return removed;
    }

    @Override
    public int clear(String chatId) {
        requireChatId(chatId);
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
     * 将消息从一种状态条件更新为另一种状态。
     * <p>Conditionally updates messages from one status to another.</p>
     *
     * @param ids  the message ids
     * @param from the required current status
     * @param to   the target status
     */
    private void updateStatus(Collection<String> ids, MessageStatus from, MessageStatus to) {
        if (ids == null || ids.isEmpty()) return;
        List<String> distinct = ids.stream()
                .filter(id -> id != null && !id.isBlank())
                .distinct()
                .toList();
        if (distinct.isEmpty()) return;
        LambdaUpdateWrapper<Message> update = new LambdaUpdateWrapper<>();
        update.in(Message::getId, distinct)
                .eq(Message::getStatus, from)
                .set(Message::getStatus, to);
        baseMapper.update(null, update);
    }

    /**
     * 将会话窗口修剪至配置长度；情景记忆开启时从最旧开始删除并在已读消息处停止，关闭时可删除已读消息。
     * <p>Trims the chat window to the configured size, stopping at a read message while episodic memory is enabled and allowing read messages to be removed otherwise.</p>
     *
     * @param chatId chat identifier
     * @return true when trimming stopped at an uncompacted read message
     */
    private boolean trimMessages(String chatId) {
        int keep = windowSize();
        List<Message> all = lambdaQuery()
                .select(Message::getId, Message::getStatus)
                .eq(Message::getChatId, chatId)
                .orderByDesc(Message::getCreatedAt)
                .orderByDesc(Message::getId)
                .list();
        if (all.size() <= keep) return false;
        List<Message> excess = all.subList(keep, all.size());
        List<String> ids = new ArrayList<>();
        boolean blocked = false;
        Boolean protectRead = null;
        for (int i = excess.size() - 1; i >= 0; i--) {
            Message message = excess.get(i);
            if (message.getStatus() == MessageStatus.READ) {
                if (protectRead == null)
                    protectRead = memoryService.isEpisodicEnabled(chatId);
                if (protectRead) {
                    blocked = true;
                    break;
                }
            }
            ids.add(message.getId());
        }
        if (!ids.isEmpty()) remove(ids);
        return blocked;
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

    /**
     * 选择会话对应的条带锁。
     * <p>Selects the striped lock for the chat.</p>
     *
     * @param chatId chat identifier
     * @return the striped lock
     */
    private ReentrantLock lock(String chatId) {
        int hash = chatId == null ? 0 : chatId.hashCode();
        return locks[Math.floorMod(hash, locks.length)];
    }

    /**
     * 创建固定数量的条带锁。
     * <p>Creates the fixed set of striped locks.</p>
     *
     * @return the striped locks
     */
    private static ReentrantLock[] initLocks() {
        ReentrantLock[] locks = new ReentrantLock[LOCK_STRIPES];
        for (int i = 0; i < locks.length; i++) locks[i] = new ReentrantLock();
        return locks;
    }

    /**
     * 校验会话标识。
     * <p>Validates the chat identifier.</p>
     *
     * @param chatId chat identifier
     */
    private static void requireChatId(String chatId) {
        if (chatId == null || chatId.isBlank())
            throw new MessageException("Missing key: chatId");
    }
}
