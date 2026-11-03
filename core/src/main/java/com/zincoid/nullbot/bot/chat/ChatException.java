package com.zincoid.nullbot.bot.chat;

import com.zincoid.nullbot.platform.exception.BotException;

/**
 * 聊天流水线无法处理事件时抛出。
 * <p>Thrown when the chat pipeline cannot handle an event.</p>
 */
public class ChatException extends BotException {

    public ChatException(String message) {
        super(message);
    }

    public ChatException(String message, Throwable cause) {
        super(message, cause);
    }
}
