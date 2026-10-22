package com.zincoid.nullbot.module.ai.memory;

import com.zincoid.nullbot.module.ai.AiException;
import com.zincoid.nullbot.module.ai.message.AiMessage;
import com.zincoid.nullbot.module.message.MessageWindowStore;
import com.zincoid.nullbot.platform.persistence.enums.MessageRole;
import com.zincoid.nullbot.platform.persistence.po.Message;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 基于消息窗口的 AI 记忆实现：历史复用 message 表，按 role 区分用户与助手。
 * <p>AI memory over the message window: history reuses the message table, with role distinguishing user and assistant.</p>
 */
@Component
@RequiredArgsConstructor
public class MessageWindowMemory implements Memory {

    private final MessageWindowStore messageWindowStore;

    // ════ Get ═════════════════════

    @Override
    public List<AiMessage> get(String chatId) {
        if (chatId == null || chatId.isBlank()) throw new AiException("Missing key: chatId");
        return messageWindowStore.get(chatId).stream()
                .map(this::toAiMessage)
                .toList();
    }

    // ════ Add ═════════════════════

    @Override
    public void add(String chatId, String messageId, String userId, String nickname, AiMessage message) {
        if (chatId == null || chatId.isBlank()) throw new AiException("Missing key: chatId");
        if (messageId == null || messageId.isBlank()) throw new AiException("Missing key: messageId");
        if (userId == null || userId.isBlank()) throw new AiException("Missing key: userId");
        if (nickname == null) throw new AiException("Missing key: nickname");
        if (message == null) throw new AiException("Missing key: message");
        String content = message.getContent();
        if (content == null || content.isBlank()) throw new AiException("Missing content");
        MessageRole role = switch (message.getRole()) {
            case USER -> MessageRole.USER;
            case ASSISTANT -> MessageRole.ASSISTANT;
            default -> throw new AiException("Unsupported role: " + message.getRole());
        };
        messageWindowStore.add(chatId, messageId, userId, role, nickname,
                content, message.getReasoning(), List.of());
    }

    // ════ Helpers ═════════════════

    private AiMessage toAiMessage(Message message) {
        String content = message.getContent() == null || message.getContent().isEmpty()
                ? "(No content)"
                : message.getContent();
        if (message.getRole() == MessageRole.ASSISTANT)
            return AiMessage.assistant(content).withReasoning(message.getReasoning());
        String name = message.getNickname() == null ? message.getUserId() : message.getNickname();
        return AiMessage.user(name, "[id=" + message.getId() + "] " + content, List.of());
    }
}
