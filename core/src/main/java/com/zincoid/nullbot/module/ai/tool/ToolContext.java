package com.zincoid.nullbot.module.ai.tool;

import java.util.List;

/**
 * 工具执行上下文：向当前会话发送消息与执行指令的能力。
 * <p>Tool execution context: the ability to send messages to the current chat and run commands.</p>
 */
public interface ToolContext {

    /**
     * 本轮是否已经通过发送工具发出最终消息。
     * <p>Whether the final message of the turn has already been sent through the send tool.</p>
     *
     * @return true when no separate final reply is needed
     */
    boolean hasFinalSent();

    /**
     * 向当前会话发送一条消息，可标记为本轮最终消息。
     * <p>Sends one message to the current chat, optionally marking it as the final message of the turn.</p>
     *
     * @param text    the message text, may be blank when files are present
     * @param fileIds the file ids of media to attach
     * @param replyId the platform message id to quote, nullable
     * @param last    whether this message completes the turn
     * @return the tool result
     */
    ToolRes send(String text, List<Long> fileIds, String replyId, boolean last);

    /**
     * 禁言或解除禁言当前群的成员。
     * <p>Mutes or unmutes a member of the current group.</p>
     *
     * @param userId  target platform user id
     * @param minutes duration in minutes, 0 to unmute
     * @return the tool result
     */
    ToolRes mute(String userId, int minutes);

    /**
     * 撤回一条消息。
     * <p>Recalls a message.</p>
     *
     * @param messageId platform message id
     * @return the tool result
     */
    ToolRes recall(String messageId);

    /**
     * 执行机器人指令。
     * <p>Runs a bot command.</p>
     *
     * @param name   command name
     * @param params command parameters
     * @return the tool result
     */
    ToolRes cmd(String name, List<String> params);
}
