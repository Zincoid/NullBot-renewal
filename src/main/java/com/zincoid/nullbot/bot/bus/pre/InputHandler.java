package com.zincoid.nullbot.bot.bus.pre;

import com.zincoid.nullbot.bot.spi.event.BotEvent;
import com.zincoid.nullbot.bot.spi.event.GroupMsgEvent;
import com.zincoid.nullbot.bot.spi.event.PrivateMsgEvent;
import com.zincoid.nullbot.bot.spi.model.MsgContent;
import com.zincoid.nullbot.bot.spi.model.MsgSegment;
import com.zincoid.nullbot.bot.spi.port.BotClient;
import com.zincoid.nullbot.module.system.InputManager;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 输入处理器：把群聊/私聊消息交付给等待中的输入，命中时消费事件。
 * <p>Input handler: delivers group and private messages to pending inputs and consumes the event on a match.</p>
 */
@Order(1)
@Component
@RequiredArgsConstructor
public class InputHandler implements PreHandler {

    private final InputManager inputManager;

    // ════ Handle ══════════════════

    @Override
    public void handle(BotClient client, BotEvent event, PreChain chain) {
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

    private boolean response(String groupId, String userId, MsgContent content) {
        String text = text(content);
        return !text.isBlank() && inputManager.response(groupId, userId, text);
    }

    private boolean response(String userId, MsgContent content) {
        String text = text(content);
        return !text.isBlank() && inputManager.response(userId, text);
    }

    private static String text(MsgContent content) {
        StringBuilder text = new StringBuilder();
        for (MsgSegment segment : content.segments())
            if (segment instanceof MsgSegment.Text(String s)) text.append(s);
        return text.toString();
    }
}
