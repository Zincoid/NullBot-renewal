package com.zincoid.nullbot.module.ai.message;

/**
 * 系统消息。
 * <p>System message.</p>
 *
 * @param content the system prompt
 */
public record SystemMsg(String content) implements AiMsg {
}
