package com.zincoid.nullbot.bot.bus;

import com.zincoid.nullbot.bot.bus.middleware.Middleware;
import com.zincoid.nullbot.bot.bus.middleware.MiddlewareChain;
import com.zincoid.nullbot.bot.cmd.CmdDispatcher;
import com.zincoid.nullbot.bot.spi.event.BotEvent;
import com.zincoid.nullbot.bot.spi.event.GroupMsgEvent;
import com.zincoid.nullbot.bot.spi.event.GroupPokeEvent;
import com.zincoid.nullbot.bot.spi.event.GroupRecallEvent;
import com.zincoid.nullbot.bot.spi.event.JoinEvent;
import com.zincoid.nullbot.bot.spi.event.LeaveEvent;
import com.zincoid.nullbot.bot.spi.event.PrivateMsgEvent;
import com.zincoid.nullbot.bot.spi.event.PrivatePokeEvent;
import com.zincoid.nullbot.bot.spi.event.PrivateRecallEvent;
import com.zincoid.nullbot.bot.spi.port.BotClient;
import com.zincoid.nullbot.bot.spi.port.BotServer;
import com.zincoid.nullbot.module.common.GroupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 入站事件分发入口。
 * <p>Inbound event dispatch entry point.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BotDispatcher implements BotServer {

    private final List<Middleware> middlewares;
    private final CmdDispatcher dispatcher;
    private final BotLifecycleTracker lifecycleTracker;
    private final GroupService groupService;

    /**
     * 处理入站事件：按当前在线账号过滤，再经过中间件链和指令分发。
     * <p>Handles an inbound event by filtering against the online account, then running the middleware chain and command dispatch.</p>
     *
     * @param client the outbound port
     * @param event  the inbound event
     */
    @Async("AsyncRunner")
    @Override
    public void handle(BotClient client, BotEvent event) {
        try {
            String online = lifecycleTracker.onlineId();
            if (online == null || !online.equals(event.selfId())) {
                log.info("Event ignored: account={}, online={}", event.selfId(), online);
                return;
            }
            String groupId = groupId(event);
            if (groupId != null && groupService.isBanned(groupId)) {
                log.info("Group banned: {}", groupId);
                return;
            }
            log.info("Event dispatched: {}", event.getClass());
            if (new MiddlewareChain(middlewares).process(client, event)) return;
            dispatcher.dispatch(client, event);
        } catch (Exception e) {
            log.error("Event failed: {}", e.getMessage(), e);
        }
    }

    // ════ Helpers ═════════════════

    /**
     * 读取事件所属群 ID，私聊事件返回 null。
     * <p>Returns the group id of the event, or null for private events.</p>
     *
     * @param event the inbound event
     * @return the group id, or null
     */
    private static String groupId(BotEvent event) {
        return switch (event) {
            case GroupMsgEvent e -> e.groupId();
            case GroupPokeEvent e -> e.groupId();
            case GroupRecallEvent e -> e.groupId();
            case JoinEvent e -> e.groupId();
            case LeaveEvent e -> e.groupId();
            case PrivateMsgEvent _, PrivatePokeEvent _, PrivateRecallEvent _ -> null;
        };
    }
}
