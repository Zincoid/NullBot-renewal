package com.zincoid.nullbot.module.ai.tool;

/**
 * 模型发起的一次工具调用。
 * <p>One tool call issued by the model.</p>
 *
 * @param id   the provider tool call id
 * @param name the tool name
 * @param args the raw JSON arguments
 */
public record ToolCall(String id, String name, String args) {
}
