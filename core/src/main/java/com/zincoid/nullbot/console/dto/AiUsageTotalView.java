package com.zincoid.nullbot.console.dto;

/**
 * 控制台全量 AI 用量视图。
 * <p>Console view of the all-time AI usage.</p>
 *
 * @param total        the all-time AI turn count
 * @param inputTokens  the all-time input token count
 * @param outputTokens the all-time output token count
 */
public record AiUsageTotalView(long total, long inputTokens, long outputTokens) {
}
