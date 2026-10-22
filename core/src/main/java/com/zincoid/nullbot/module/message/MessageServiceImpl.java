package com.zincoid.nullbot.module.message;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.zincoid.nullbot.platform.persistence.enums.MessageRole;
import com.zincoid.nullbot.platform.persistence.mapper.MessageMapper;
import com.zincoid.nullbot.platform.persistence.po.Message;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * {@link MessageService} 的默认实现。
 * <p>Default implementation of {@link MessageService}.</p>
 */
@Slf4j
@Service
public class MessageServiceImpl extends ServiceImpl<MessageMapper, Message> implements MessageService {

    // ════ Save ════════════════════

    @Override
    public Message save(String id, String chatId, String userId, MessageRole role, String nickname,
                        String content, String reasoning, List<String> files) {
        if (id == null || id.isBlank()) throw new MessageException("Missing key: id");
        if (chatId == null || chatId.isBlank()) throw new MessageException("Missing key: chatId");
        if (userId == null || userId.isBlank()) throw new MessageException("Missing key: userId");
        if (role == null) throw new MessageException("Missing key: role");
        if (nickname == null) throw new MessageException("Missing key: nickname");
        boolean noContent = content == null || content.isEmpty();
        boolean noFiles = files == null || files.isEmpty();
        if (noContent && noFiles) throw new MessageException("Missing content and files");
        Message message = Message.builder()
                .id(id)
                .chatId(chatId)
                .userId(userId)
                .role(role)
                .nickname(nickname)
                .content(noContent ? null : content)
                .reasoning(reasoning == null || reasoning.isEmpty() ? null : reasoning)
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
        if (!removeById(id)) throw new MessageException("Message not found: " + id);
        log.info("Message deleted: {}", id);
    }
}
