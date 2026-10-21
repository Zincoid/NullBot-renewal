package com.zincoid.nullbot.module.ai.tool;

/**
 * 工具执行上下文：向当前会话发送消息的能力。
 * <p>Tool execution context: the ability to send messages to the current chat.</p>
 */
public interface ToolContext {

    /**
     * 向当前会话发送一条消息。
     * <p>Sends one message to the current chat.</p>
     *
     * @param text    the message text
     * @param replyId the platform message id to quote, nullable
     * @return the sent platform message id
     */
    String send(String text, String replyId);
}
