package com.zincoid.nullbot.bot.bus.middleware;

import com.zincoid.nullbot.bot.cmd.input.InputManager;
import com.zincoid.nullbot.bot.spi.event.BotEvent;
import com.zincoid.nullbot.bot.spi.event.GroupMsgEvent;
import com.zincoid.nullbot.bot.spi.event.PrivateMsgEvent;
import com.zincoid.nullbot.bot.spi.model.MsgContent;
import com.zincoid.nullbot.bot.spi.port.BotClient;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 输入监听中间件：把群聊/私聊消息交付给等待中的输入，命中时消费事件。
 * <p>Input listening middleware: delivers group and private messages to pending inputs and consumes the event on a match.</p>
 */
@Order(1)
@Component
@RequiredArgsConstructor
public class InputListenMiddleware implements Middleware {

    private final InputManager inputManager;

    // ════ Handle ══════════════════

    /**
     * 把消息交付给等待中的输入，命中时消费事件。
     * <p>Delivers messages to pending inputs and consumes the event on a match.</p>
     *
     * @param client the outbound port
     * @param event  the inbound event
     * @param chain  the remaining middleware chain
     */
    @Override
    public void handle(BotClient client, BotEvent event, MiddlewareChain chain) {
        if (event instanceof GroupMsgEvent e) {
            if (response(e.groupId(), e.userId(), e.content())) {
                chain.consume();
                return;
            }
        }
        if (event instanceof PrivateMsgEvent e) {
            if (response(e.userId(), e.content())) {
                chain.consume();
                return;
            }
        }
        chain.doHandle(client, event);
    }

    // ════ Helpers ═════════════════

    /**
     * 把群消息交付给等待中的输入。
     * <p>Delivers a group message to pending inputs.</p>
     *
     * @param groupId group platform id
     * @param userId  sender platform user id
     * @param content the message content
     * @return true when a wait accepted the message
     */
    private boolean response(String groupId, String userId, MsgContent content) {
        String text = content.text();
        return !text.isBlank() && inputManager.response(groupId, userId, text);
    }

    /**
     * 把私聊消息交付给该用户的等待输入。
     * <p>Delivers a private message to the user's pending input.</p>
     *
     * @param userId  sender platform user id
     * @param content the message content
     * @return true when a wait accepted the message
     */
    private boolean response(String userId, MsgContent content) {
        String text = content.text();
        return !text.isBlank() && inputManager.response(userId, text);
    }
}
