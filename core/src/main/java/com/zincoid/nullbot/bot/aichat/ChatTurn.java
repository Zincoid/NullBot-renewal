package com.zincoid.nullbot.bot.aichat;

import com.zincoid.nullbot.bot.spi.event.BotEvent;
import com.zincoid.nullbot.bot.spi.event.GroupEvent;
import com.zincoid.nullbot.bot.spi.event.PrivateEvent;
import com.zincoid.nullbot.bot.spi.port.BotClient;
import com.zincoid.nullbot.platform.persistence.enums.ChatType;
import com.zincoid.nullbot.platform.util.ChatIds;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 单轮对话的运行时数据：出站端口、会话标识、触发事件与轮次状态。
 * <p>Per-turn conversation data: outbound port, chat identifiers, triggering event, and turn state.</p>
 *
 * @param client   the outbound port
 * @param chatType the chat type
 * @param targetId group or user platform id
 * @param chatId   the composite chat id
 * @param event    the triggering event
 * @param state    the turn state
 */
record ChatTurn(BotClient client, ChatType chatType, String targetId,
                String chatId, BotEvent event, State state) {

    /**
     * 单轮对话状态：记录是否已通过发送工具发出最终消息。
     * <p>Per-turn conversation state: records whether the final message has been sent through the send tool.</p>
     */
    static final class State {

        private final AtomicBoolean finalSent = new AtomicBoolean();

        boolean hasFinalSent() {
            return finalSent.get();
        }

        void markFinalSent() {
            finalSent.set(true);
        }
    }

    /**
     * 由触发事件构建单轮数据。
     * <p>Builds the turn data from the triggering event.</p>
     *
     * @param client the outbound port
     * @param event  the triggering event
     * @return the turn data
     */
    static ChatTurn of(BotClient client, BotEvent event) {
        return switch (event) {
            case GroupEvent e -> new ChatTurn(client, ChatType.GROUP, e.groupId(),
                    ChatIds.ofGroup(e.groupId()), event, new State());
            case PrivateEvent e -> new ChatTurn(client, ChatType.PRIVATE, e.userId(),
                    ChatIds.ofPrivate(e.userId()), event, new State());
            default -> throw new ChatException(
                    "Unsupported event: " + event.getClass().getSimpleName());
        };
    }

    /**
     * 本轮是否已发出最终消息。
     * <p>Returns whether the final message of the turn has been sent.</p>
     *
     * @return true when the final message has been sent
     */
    boolean hasFinalSent() {
        return state.hasFinalSent();
    }

    /**
     * 标记本轮已发出最终消息。
     * <p>Marks the final message of the turn as sent.</p>
     */
    void markFinalSent() {
        state.markFinalSent();
    }
}
