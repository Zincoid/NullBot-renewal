package com.zincoid.nullbot.console.dto;

import com.zincoid.nullbot.platform.persistence.po.Message;

import java.time.LocalDateTime;

/**
 * 控制台消息视图。
 * <p>Console view of a retained message.</p>
 *
 * @param id              the platform message id
 * @param chatId          the composite chat id
 * @param userId          the sender platform user id
 * @param role            the role label
 * @param nickname        the sender nickname
 * @param content         the text content
 * @param reasoning       the assistant reasoning content, nullable
 * @param createdAt       the creation time
 * @param attachmentCount the attachment count
 */
public record MessageView(
        String id,
        String chatId,
        String userId,
        String role,
        String nickname,
        String content,
        String reasoning,
        LocalDateTime createdAt,
        long attachmentCount
) {

    /**
     * 由消息记录构建视图。
     * <p>Builds the view from a message record.</p>
     *
     * @param message         the message record
     * @param attachmentCount the attachment count
     * @return the view
     */
    public static MessageView of(Message message, long attachmentCount) {
        return new MessageView(
                message.getId(),
                message.getChatId(),
                message.getUserId(),
                message.getRole() == null ? null : message.getRole().getLabel(),
                message.getNickname(),
                message.getContent(),
                message.getReasoning(),
                message.getCreatedAt(),
                attachmentCount);
    }
}
