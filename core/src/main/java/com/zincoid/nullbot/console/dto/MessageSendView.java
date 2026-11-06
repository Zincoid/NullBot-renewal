package com.zincoid.nullbot.console.dto;

/**
 * 控制台发送结果视图。
 * <p>Console view of a sent message result.</p>
 *
 * @param id     the platform message id
 * @param stored whether the message was recorded in history
 */
public record MessageSendView(String id, boolean stored) {
}
