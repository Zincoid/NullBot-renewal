package com.zincoid.nullbot.bot.bus;

import com.zincoid.nullbot.bot.bus.middleware.Middleware;
import com.zincoid.nullbot.bot.bus.middleware.MiddlewareChain;
import com.zincoid.nullbot.bot.cmd.CmdDispatcher;
import com.zincoid.nullbot.bot.spi.event.BotEvent;
import com.zincoid.nullbot.bot.spi.port.BotClient;
import com.zincoid.nullbot.bot.spi.port.BotServer;
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
            log.info("Event dispatched: {}", event.getClass());
            if (new MiddlewareChain(middlewares).process(client, event)) return;
            dispatcher.dispatch(client, event);
        } catch (Exception e) {
            log.error("Event failed: {}", e.getMessage(), e);
        }
    }
}
