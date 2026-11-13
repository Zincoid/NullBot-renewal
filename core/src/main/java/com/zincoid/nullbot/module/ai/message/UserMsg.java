package com.zincoid.nullbot.module.ai.message;

import java.util.List;

/**
 * 用户消息。
 * <p>User message.</p>
 *
 * @param name    the sender name, nullable
 * @param content the message text
 * @param images  the image urls, never null
 */
public record UserMsg(String name, String content, List<String> images) implements AiMsg {

    public UserMsg {
        images = images == null ? List.of() : List.copyOf(images);
    }
}
