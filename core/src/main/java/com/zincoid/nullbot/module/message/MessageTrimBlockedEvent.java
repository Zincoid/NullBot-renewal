package com.zincoid.nullbot.module.message;

/**
 * 窗口修剪因遇到未压缩的已读消息而停止的事件。
 * <p>Event raised when window trimming stops at an uncompacted read message.</p>
 *
 * @param chatId chat identifier
 */
public record MessageTrimBlockedEvent(String chatId) {
}
