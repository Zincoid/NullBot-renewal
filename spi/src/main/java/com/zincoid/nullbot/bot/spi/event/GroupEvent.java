package com.zincoid.nullbot.bot.spi.event;

/**
 * 群聊作用域事件：提供非空的群 ID。
 * <p>Group-scoped event: provides a non-null group id.</p>
 */
public interface GroupEvent {

    /**
     * 事件所属群 ID。
     * <p>The group id of the event.</p>
     *
     * @return the group id
     */
    String groupId();
}
