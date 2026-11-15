package com.zincoid.nullbot.module.ai.memory;

import com.zincoid.nullbot.module.ai.message.AiMsg;

import java.util.List;

/**
 * 工作记忆：按会话读取与保存尚未压缩的原始对话消息。
 * <p>Working memory: reads and saves uncompacted raw conversation messages per chat.</p>
 */
public interface WorkingMemory {

    /**
     * 读取会话中尚未压缩的历史，并标记为已读。
     * <p>Returns the uncompacted conversation history and marks it as read.</p>
     *
     * @param chatId chat identifier
     * @return the history, oldest first
     */
    List<AiMsg> read(String chatId);

    /**
     * 保存一条会话消息。
     * <p>Saves one conversation message.</p>
     *
     * @param chatId    chat identifier
     * @param messageId the platform message id
     * @param userId    the sender platform user id
     * @param nickname  the sender nickname
     * @param message   the message
     */
    void save(String chatId, String messageId, String userId, String nickname, AiMsg message);
}
