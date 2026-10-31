package com.zincoid.nullbot.bot.spi.event;

/**
 * 私聊作用域事件：标记私聊消息、戳一戳与撤回事件。
 * <p>Private-scoped event: marks private messages, pokes, and recall events.</p>
 */
public interface PrivateEvent {

    /**
     * 事件关联用户 ID。
     * <p>The user id associated with the event.</p>
     *
     * @return the user id
     */
    String userId();
}
