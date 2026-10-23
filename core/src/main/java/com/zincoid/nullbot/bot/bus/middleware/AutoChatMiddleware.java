package com.zincoid.nullbot.bot.bus.middleware;

import com.zincoid.nullbot.bot.cmd.CmdDispatcher;
import com.zincoid.nullbot.bot.cmd.CmdParser;
import com.zincoid.nullbot.bot.cmd.CmdRequest;
import com.zincoid.nullbot.bot.spi.event.BotEvent;
import com.zincoid.nullbot.bot.spi.event.GroupMsgEvent;
import com.zincoid.nullbot.bot.spi.port.BotClient;
import com.zincoid.nullbot.module.setting.GroupSettingService;
import com.zincoid.nullbot.platform.persistence.po.GroupSetting;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 自动聊天中间件：按群设置的概率把普通群消息派发为 Chat 指令。
 * <p>Auto-chat middleware: dispatches ordinary group messages as Chat commands with the probability configured for the group.</p>
 */
@Slf4j
@Order(3)
@Component
@RequiredArgsConstructor
public class AutoChatMiddleware implements Middleware {

    private final GroupSettingService groupSettingService;
    private final CmdParser cmdParser;
    private final CmdDispatcher cmdDispatcher;

    // ════ Handle ══════════════════

    /**
     * 按群设置决定是否自动聊天，命中时派发 Chat 指令并消费事件。
     * <p>Decides whether to chat automatically from the group settings, dispatching Chat and consuming the event on a hit.</p>
     *
     * @param client the outbound port
     * @param event  the inbound event
     * @param chain  the remaining middleware chain
     */
    @Override
    public void handle(BotClient client, BotEvent event, MiddlewareChain chain) {
        if (event instanceof GroupMsgEvent e && shouldChat(e)) {
            cmdDispatcher.dispatch(client, event, new CmdRequest(CmdParser.CHAT_CMD_NAME, List.of()));
            chain.consume();
            return;
        }
        chain.doHandle(client, event);
    }

    // ════ Helpers ═════════════════

    /**
     * 判断普通群消息是否触发自动聊天。
     * <p>Whether an ordinary group message triggers an automatic chat.</p>
     *
     * @param event the group message event
     * @return true when the message should trigger an automatic chat
     */
    private boolean shouldChat(GroupMsgEvent event) {
        if (event.selfId().equals(event.userId())) return false;
        if (event.at()) return false;
        GroupSetting setting = groupSettingService.get(event.groupId());
        if (!setting.getAiAutoChat()) return false;
        double frequency = setting.getAiChatFrequency();
        if (!(ThreadLocalRandom.current().nextDouble() < frequency)) return false;
        if (cmdParser.isCmd(event.content())) return false;
        log.info("Auto chat: group={}, user={}", event.groupId(), event.userId());
        return true;
    }
}
