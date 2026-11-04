package com.zincoid.nullbot.bot.bus;

import com.zincoid.nullbot.bot.bus.middleware.Middleware;
import com.zincoid.nullbot.bot.bus.middleware.MiddlewareChain;
import com.zincoid.nullbot.bot.cmd.CmdDispatcher;
import com.zincoid.nullbot.bot.spi.event.BotEvent;
import com.zincoid.nullbot.bot.spi.event.GroupEvent;
import com.zincoid.nullbot.bot.spi.event.MsgEvent;
import com.zincoid.nullbot.bot.spi.port.BotClient;
import com.zincoid.nullbot.bot.spi.port.BotServer;
import com.zincoid.nullbot.module.common.GroupService;
import com.zincoid.nullbot.module.common.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 入站事件分发入口：过滤后运行中间件链，未消费的消息事件交给指令分发。
 * <p>Inbound event dispatch entry point: filters events, runs the middleware chain, and dispatches unconsumed messages.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BotDispatcher implements BotServer {

    private final BotLifecycleTracker lifecycleTracker;
    private final GroupService groupService;
    private final UserService userService;
    private final List<Middleware> middlewares;
    private final CmdDispatcher cmdDispatcher;

    /**
     * 异步处理入站事件。
     * <p>Handles an inbound event asynchronously.</p>
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
                log.debug("Event ignored: self={}", event.selfId());
                return;
            }
            if (event instanceof GroupEvent e
                    && groupService.isBanned(e.groupId())) {
                log.debug("Event banned: group={}", e.groupId());
                return;
            }
            if (userService.isBanned(event.userId())) {
                log.debug("Event banned: user={}", event.userId());
                return;
            }
            log.debug("Event dispatched: {}", event);
            if (new MiddlewareChain(middlewares).process(client, event)) return;
            if (event instanceof MsgEvent e) cmdDispatcher.dispatch(client, e);
        } catch (Exception e) {
            log.error("Event failed: {}", e.getMessage(), e);
        }
    }
}
