package com.zincoid.nullbot.module.ai.message;

import com.zincoid.nullbot.module.ai.tool.ToolCall;

import java.util.List;

/**
 * 模型对话消息：系统、用户、助手与工具四类消息。
 * <p>Model chat message: system, user, assistant, and tool messages.</p>
 */
public sealed interface AiMsg permits
        SystemMsg,
        UserMsg,
        AssistantMsg,
        ToolMsg {

    // ════ Factories ══════════════

    /**
     * 创建系统消息。
     * <p>Creates a system message.</p>
     *
     * @param content the system prompt
     * @return the system message
     */
    static AiMsg system(String content) {
        return new SystemMsg(content);
    }

    /**
     * 创建用户消息。
     * <p>Creates a user message.</p>
     *
     * @param content the message text
     * @return the user message
     */
    static AiMsg user(String content) {
        return user(null, content, null);
    }

    /**
     * 创建带名称与图片的用户消息。
     * <p>Creates a user message with a name and images.</p>
     *
     * @param name    the sender name, nullable
     * @param content the message text
     * @param images  the image urls, nullable
     * @return the user message
     */
    static AiMsg user(String name, String content, List<String> images) {
        return new UserMsg(name, content, images);
    }

    /**
     * 创建助手文本消息。
     * <p>Creates an assistant message with text content.</p>
     *
     * @param content the message text
     * @return the assistant message
     */
    static AssistantMsg assistant(String content) {
        return new AssistantMsg(content, null, null);
    }

    /**
     * 创建助手工具调用消息。
     * <p>Creates an assistant message with tool calls.</p>
     *
     * @param tcs the tool calls
     * @return the assistant message
     */
    static AssistantMsg assistant(List<ToolCall> tcs) {
        return new AssistantMsg(null, tcs, null);
    }

    /**
     * 创建工具结果消息。
     * <p>Creates a tool result message.</p>
     *
     * @param tcId    the tool call id
     * @param content the tool result text
     * @return the tool message
     */
    static AiMsg tool(String tcId, String content) {
        return new ToolMsg(tcId, content);
    }
}
