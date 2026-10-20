package com.zincoid.nullbot.module.common;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.zincoid.nullbot.platform.persistence.mapper.MessageMapper;
import com.zincoid.nullbot.platform.persistence.po.Message;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Default implementation of {@link MessageService}.
 * <p>{@link MessageService} 的默认实现。</p>
 */
@Slf4j
@Service
public class MessageServiceImpl extends ServiceImpl<MessageMapper, Message> implements MessageService {

    // ════ Save ════════════════════

    @Override
    public Message save(String id, String chatId, String userId, String content, List<String> files) {
        if (id == null || id.isBlank()) throw new CommonException("Missing key: id");
        if (chatId == null || chatId.isBlank()) throw new CommonException("Missing key: chatId");
        if (userId == null || userId.isBlank()) throw new CommonException("Missing key: userId");
        boolean noContent = content == null || content.isBlank();
        boolean noFiles = files == null || files.isEmpty();
        if (noContent && noFiles) throw new CommonException("Missing content and files");
        Message message = Message.builder()
                .id(id)
                .chatId(chatId)
                .userId(userId)
                .content(noContent ? null : content)
                .files(noFiles ? null : files)
                .createdAt(LocalDateTime.now())
                .build();
        save(message);
        log.info("Message saved: {}", message.getId());
        return message;
    }

    // ════ Delete ══════════════════

    @Override
    public void delete(String id) {
        if (!removeById(id)) throw new CommonException("Message not found: " + id);
        log.info("Message deleted: {}", id);
    }
}
