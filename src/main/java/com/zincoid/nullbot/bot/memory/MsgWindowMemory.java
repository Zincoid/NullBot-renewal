package com.zincoid.nullbot.bot.memory;

import com.zincoid.nullbot.module.common.CommonException;
import com.zincoid.nullbot.module.common.MessageService;
import com.zincoid.nullbot.module.config.Conf;
import com.zincoid.nullbot.module.config.ConfigException;
import com.zincoid.nullbot.module.config.ConfigService;
import com.zincoid.nullbot.platform.persistence.po.Message;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * Per-chat message window over the stored message records: each chat retains the newest N messages, trimmed on write.
 * <p>按会话划分的消息窗口：每个会话仅保留最近 N 条消息记录，写入时修剪。</p>
 */
@Component
@RequiredArgsConstructor
public class MsgWindowMemory {

    private final MessageService messageService;
    private final ConfigService configService;

    // ════ Add ═════════════════════

    /**
     * Records a message and trims the chat window to the configured size.
     * <p>记录一条消息，并将会话窗口修剪至配置长度。</p>
     *
     * @param id      platform message ID
     * @param chatId  chat identifier
     * @param userId  sender platform user ID
     * @param content text content, may be blank when files present
     * @param files   attached file paths, may be empty
     * @return the persisted record
     */
    public Message add(String id, String chatId, String userId, String content, List<String> files) {
        Message message = messageService.save(id, chatId, userId, content, files);
        trim(chatId);
        return message;
    }

    // ════ List ════════════════════

    /**
     * Returns the newest messages of the chat in chronological order.
     * <p>返回该会话最近的消息，按时间正序。</p>
     *
     * @param chatId chat identifier
     * @return the chat window, oldest first
     */
    public List<Message> list(String chatId) {
        if (chatId == null || chatId.isBlank()) throw new CommonException("Missing key: chatId");
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
     * Reads the configured window size.
     * <p>读取配置的窗口长度。</p>
     *
     * @return the window size
     */
    private int size() {
        String raw = configService.get(Conf.MESSAGE_HISTORY_MAX_LENGTH);
        int size;
        try {
            size = Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            throw new ConfigException("Malformed value: " + raw, e);
        }
        if (size <= 0) throw new ConfigException("Malformed value: " + raw);
        return size;
    }

    /**
     * Trims the chat window to the configured size.
     * <p>将会话窗口修剪至配置长度。</p>
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
        List<String> overflow = all.subList(size, all.size()).stream().map(Message::getId).toList();
        messageService.removeByIds(overflow);
    }
}
