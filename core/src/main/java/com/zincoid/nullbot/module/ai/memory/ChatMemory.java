package com.zincoid.nullbot.module.ai.memory;

import com.zincoid.nullbot.module.ai.AiException;
import com.zincoid.nullbot.module.ai.message.AiMsg;
import com.zincoid.nullbot.module.ai.message.AssistantMsg;
import com.zincoid.nullbot.module.ai.message.UserMsg;
import com.zincoid.nullbot.module.message.MessageService;
import com.zincoid.nullbot.platform.persistence.enums.MessageRole;
import com.zincoid.nullbot.platform.persistence.po.Message;
import com.zincoid.nullbot.platform.util.UserLabels;
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
    public List<AiMsg> get(String chatId) {
        return messageService.window(chatId).stream()
                .map(this::toAiMsg)
                .toList();
    }

    // ════ Add ═════════════════════

    @Override
    public void add(String chatId, String messageId, String userId, String nickname,
                    AiMsg message) {
        if (message == null) throw new AiException("Missing key: message");
        Message.MessageBuilder builder = Message.builder()
                .id(messageId)
                .chatId(chatId)
                .userId(userId)
                .nickname(nickname);
        switch (message) {
            case UserMsg m -> builder
                    .role(MessageRole.USER)
                    .content(m.content());
            case AssistantMsg m -> builder
                    .role(MessageRole.ASSISTANT)
                    .content(m.content())
                    .reasoning(m.reasoning());
            default -> throw new AiException("Unsupported message: " + message);
        }
        messageService.append(builder.build());
    }

    // ════ Helpers ═════════════════

    private AiMsg toAiMsg(Message message) {
        String content = message.getContent();
        if (content == null || content.isEmpty())
            content = "(No content)";
        if (message.getRole() == MessageRole.ASSISTANT)
            return AiMsg.assistant(content)
                    .withReasoning(message.getReasoning());
        String formatted = "[id=%s] %s: %s".formatted(
                message.getId(),
                UserLabels.of(message.getNickname(), message.getUserId()),
                content
        );
        return AiMsg.user(formatted);
    }
}
