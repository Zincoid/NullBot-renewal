package com.zincoid.nullbot.module.ai.memory;

import com.zincoid.nullbot.module.ai.message.AiMessage;

import java.util.List;

/**
 * AI 聊天记忆：按会话读取与写入对话消息。
 * <p>AI chat memory: reads and writes conversation messages per chat.</p>
 */
public interface Memory {

    /**
     * 读取会话历史。
     * <p>Returns the conversation history.</p>
     *
     * @param chatId chat identifier
     * @return the history, oldest first
     */
    List<AiMessage> get(String chatId);

    /**
     * 写入一条会话消息。
     * <p>Appends one conversation message.</p>
     *
     * @param chatId    chat identifier
     * @param messageId the platform message id
     * @param userId    the sender platform user id
     * @param nickname  the sender nickname
     * @param message   the message
     */
    void add(String chatId, String messageId, String userId, String nickname, AiMessage message);
}
