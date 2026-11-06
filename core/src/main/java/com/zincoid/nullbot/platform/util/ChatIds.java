package com.zincoid.nullbot.platform.util;

import com.zincoid.nullbot.platform.persistence.enums.ChatType;

/**
 * 会话标识编码：消息记录与窗口键共用的类型前缀复合串。
 * <p>Chat identifier codec: typed-prefix composite strings shared by chat records and window keys.</p>
 */
public final class ChatIds {

    private ChatIds() {
    }

    /**
     * 构造群聊的会话标识。
     * <p>Builds the chat identifier of a group chat.</p>
     *
     * @param groupId platform group id, must not be blank
     * @return composite identifier, e.g. {@code g:100}
     */
    public static String ofGroup(String groupId) {
        return "g:" + groupId;
    }

    /**
     * 构造私聊的会话标识。
     * <p>Builds the chat identifier of a private chat.</p>
     *
     * @param userId platform user id, must not be blank
     * @return composite identifier, e.g. {@code p:2}
     */
    public static String ofPrivate(String userId) {
        return "p:" + userId;
    }

    /**
     * 解析会话标识的类型。
     * <p>Parses the type of a chat identifier.</p>
     *
     * @param chatId composite identifier, nullable
     * @return the chat type, or null when the identifier is unrecognized
     */
    public static ChatType type(String chatId) {
        if (chatId == null) return null;
        if (chatId.startsWith("g:")) return ChatType.GROUP;
        if (chatId.startsWith("p:")) return ChatType.PRIVATE;
        return null;
    }

    /**
     * 去掉会话标识的类型前缀。
     * <p>Strips the type prefix from a chat identifier.</p>
     *
     * @param chatId composite identifier, nullable
     * @return the raw platform id, or null when the identifier is unrecognized
     */
    public static String rawId(String chatId) {
        if (chatId == null) return null;
        if (chatId.startsWith("g:") || chatId.startsWith("p:")) return chatId.substring(2);
        return null;
    }
}
