package com.zincoid.nullbot.bot.aichat;

import com.zincoid.nullbot.bot.spi.event.BotEvent;
import com.zincoid.nullbot.bot.spi.event.GroupEvent;
import com.zincoid.nullbot.bot.spi.event.PrivateEvent;
import com.zincoid.nullbot.bot.spi.port.BotClient;
import com.zincoid.nullbot.platform.persistence.enums.AiSource;
import com.zincoid.nullbot.platform.persistence.enums.ChatType;
import com.zincoid.nullbot.platform.util.ChatIds;

/**
 * 单轮对话的运行时数据：出站端口、会话标识、触发事件与触发来源。
 * <p>Per-turn conversation data: outbound port, chat identifiers, triggering event, and trigger source.</p>
 *
 * @param client   the outbound port
 * @param chatType the chat type
 * @param targetId group or user platform id
 * @param chatId   the composite chat id
 * @param event    the triggering event
 * @param source   the trigger source
 */
record ChatTurn(BotClient client, ChatType chatType, String targetId,
                String chatId, BotEvent event, AiSource source) {

    /**
     * 由触发事件与来源构建单轮数据。
     * <p>Builds the turn data from the triggering event and source.</p>
     *
     * @param client the outbound port
     * @param event  the triggering event
     * @param source the trigger source
     * @return the turn data
     */
    static ChatTurn of(BotClient client, BotEvent event, AiSource source) {
        return switch (event) {
            case GroupEvent e -> new ChatTurn(client, ChatType.GROUP, e.groupId(),
                    ChatIds.ofGroup(e.groupId()), event, source);
            case PrivateEvent e -> new ChatTurn(client, ChatType.PRIVATE, e.userId(),
                    ChatIds.ofPrivate(e.userId()), event, source);
            default -> throw new ChatException(
                    "Unsupported event: " + event.getClass().getSimpleName());
        };
    }
}
