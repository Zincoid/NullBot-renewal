package com.zincoid.nullbot.platform.util;

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
     * @param groupId platform group ID, must not be blank
     * @return composite identifier, e.g. {@code g:100}
     */
    public static String ofGroup(String groupId) {
        return "g:" + groupId;
    }

    /**
     * 构造私聊的会话标识。
     * <p>Builds the chat identifier of a private chat.</p>
     *
     * @param userId platform user ID, must not be blank
     * @return composite identifier, e.g. {@code p:2}
     */
    public static String ofPrivate(String userId) {
        return "p:" + userId;
    }
}
