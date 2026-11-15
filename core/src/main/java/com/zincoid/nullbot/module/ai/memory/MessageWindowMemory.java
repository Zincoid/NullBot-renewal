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
 * 基于消息记录的工作记忆：读取尚未压缩的消息并标记为已读，保存新的会话消息。
 * <p>Working memory over message records: reads uncompacted messages, marks them as read, and saves new conversation messages.</p>
 */
@Component
@RequiredArgsConstructor
public class MessageWindowMemory implements WorkingMemory {

    private final MessageService messageService;

    // ════ Read ════════════════════

    @Override
    public List<AiMsg> read(String chatId) {
        return messageService.callLocked(chatId, () -> {
            List<Message> messages = messageService.getUncompacted(chatId);
            if (!messages.isEmpty())
                messageService.markRead(messages.stream().map(Message::getId).toList());
            return messages.stream()
                    .map(this::toAiMsg)
                    .toList();
        });
    }

    // ════ Save ════════════════════

    @Override
    public void save(String chatId, String messageId, String userId, String nickname,
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
        messageService.record(builder.build());
    }

    // ════ Helpers ═════════════════

    /**
     * 把消息记录转换为模型消息：助手消息保留 reasoning，用户消息附带 id 与昵称。
     * <p>Converts a message record into a model message: assistant messages keep the reasoning, user messages carry the id and nickname.</p>
     *
     * @param message the message record
     * @return the model message
     */
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
