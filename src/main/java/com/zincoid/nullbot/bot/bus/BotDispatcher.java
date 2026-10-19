package com.zincoid.nullbot.bot.bus;

import com.zincoid.nullbot.bot.spi.event.BotEvent;
import com.zincoid.nullbot.bot.spi.port.BotClient;
import com.zincoid.nullbot.bot.spi.port.BotServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Inbound event dispatch entry point.
 * <p>入站事件分发入口。</p>
 * <p>Skeleton stage: only logs events; business chains (CmdProcessor / BotMonitor) hook in later.</p>
 * <p>骨架阶段：仅记录事件日志；业务链（CmdProcessor / BotMonitor）后续接入。</p>
 */
@Slf4j
@Component
public class BotDispatcher implements BotServer {

    @Override
    public void handle(BotClient client, BotEvent event) {
        log.info("Event received: {}", event);
    }
}
