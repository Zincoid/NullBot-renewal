package com.zincoid.nullbot.bot.bus;

import com.zincoid.nullbot.bot.spi.event.BotEvent;
import com.zincoid.nullbot.bot.spi.port.BotClient;
import com.zincoid.nullbot.bot.spi.port.BotServer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Inbound event dispatch entry point.
 * <p>入站事件分发入口。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BotDispatcher implements BotServer {

    private final CmdProcessor processor;

    @Override
    public void handle(BotClient client, BotEvent event) {
        try {
            log.info("Event dispatched: {}", event.getClass());
            processor.process(client, event);
        } catch (Exception e) {
            log.error("Event failed: {}", e.getMessage(), e);
        }
    }
}
