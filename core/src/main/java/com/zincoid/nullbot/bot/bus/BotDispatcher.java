package com.zincoid.nullbot.bot.bus;

import com.zincoid.nullbot.bot.spi.event.BotEvent;
import com.zincoid.nullbot.bot.spi.port.BotClient;
import com.zincoid.nullbot.bot.spi.port.BotServer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 入站事件分发入口。
 * <p>Inbound event dispatch entry point.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BotDispatcher implements BotServer {

    private final PreProcessor preProcessor;
    private final CmdProcessor cmdProcessor;

    @Async("AsyncRunner")
    @Override
    public void handle(BotClient client, BotEvent event) {
        try {
            log.info("Event dispatched: {}", event.getClass());
            if (preProcessor.process(client, event)) return;
            cmdProcessor.process(client, event);
        } catch (Exception e) {
            log.error("Event failed: {}", e.getMessage(), e);
        }
    }
}
