package com.zincoid.nullbot.module.ai.context;

/**
 * 文本信息包：按顺序拼接为系统提示词。
 * <p>Text packet: joined in order into the system prompt.</p>
 *
 * @param text the text content, nullable
 */
public record TextPacket(String text) implements ContextPacket {
}
