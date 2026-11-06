package com.zincoid.nullbot.module.ai.memory;

import com.zincoid.nullbot.module.ai.AiException;
import com.zincoid.nullbot.module.ai.message.AiMessage;
import com.zincoid.nullbot.module.message.MessageService;
import com.zincoid.nullbot.platform.persistence.enums.MessageRole;
import com.zincoid.nullbot.platform.persistence.po.Message;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 基于消息记录的 AI 聊天记忆实现：历史复用 message 表，按 role 区分用户与助手。
 * <p>AI chat memory over message records: history reuses the message table, with role distinguishing user and assistant.</p>
 */
@Component
@RequiredArgsConstructor
public class ChatMemory implements Memory {

    private final MessageService messageService;

    // ════ Get ═════════════════════

    @Override
    public List<AiMessage> get(String chatId) {
        return messageService.window(chatId).stream()
                .map(this::toAiMessage)
                .toList();
    }

    // ════ Add ═════════════════════

    @Override
    public void add(String chatId, String messageId, String userId, String nickname,
                    AiMessage message, List<Long> files) {
        if (message == null) throw new AiException("Missing key: message");
        String content = message.getContent();
        if (content == null || content.isBlank()) throw new AiException("Missing content");
        MessageRole role = switch (message.getRole()) {
            case USER -> MessageRole.USER;
            case ASSISTANT -> MessageRole.ASSISTANT;
            default -> throw new AiException("Unsupported role: " + message.getRole());
        };
        messageService.append(Message.builder()
                .id(messageId)
                .chatId(chatId)
                .userId(userId)
                .nickname(nickname)
                .role(role)
                .content(content)
                .reasoning(message.getReasoning())
                .files(files)
                .build());
    }

    // ════ Helpers ═════════════════

    private AiMessage toAiMessage(Message message) {
        String content = message.getContent();
        if (content == null || content.isEmpty()) content = "(No content)";
        if (message.getRole() == MessageRole.ASSISTANT)
            return AiMessage.assistant(content).withReasoning(message.getReasoning());
        String name = message.getNickname();
        if (name == null || name.isBlank()) name = message.getUserId();
        return AiMessage.user("[id=%s] %s(%s): %s".formatted(
                message.getId(), name, message.getUserId(), content));
    }
}
