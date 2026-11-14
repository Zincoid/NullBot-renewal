package com.zincoid.nullbot.module.ai.memory;

import com.zincoid.nullbot.module.ai.message.AiMsg;

import java.util.List;

/**
 * 工作记忆：按会话读取与写入最近的原始对话消息。
 * <p>Working memory: reads and writes the recent raw conversation messages per chat.</p>
 */
public interface WorkingMemory {

    /**
     * 读取会话历史。
     * <p>Returns the conversation history.</p>
     *
     * @param chatId chat identifier
     * @return the history, oldest first
     */
    List<AiMsg> get(String chatId);

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
    void add(String chatId, String messageId, String userId, String nickname, AiMsg message);
}
