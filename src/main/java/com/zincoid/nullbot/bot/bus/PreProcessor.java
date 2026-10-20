package com.zincoid.nullbot.bot.bus;

import com.zincoid.nullbot.bot.bus.pre.PreChain;
import com.zincoid.nullbot.bot.bus.pre.PreHandler;
import com.zincoid.nullbot.bot.spi.event.BotEvent;
import com.zincoid.nullbot.bot.spi.port.BotClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Runs the event pre-processing chain ahead of command dispatch.
 * <p>在指令分发之前运行事件预处理链。</p>
 */
@Component
@RequiredArgsConstructor
public class PreProcessor {

    private final List<PreHandler> handlers;

    /**
     * Processes an inbound event through the pre-handler chain.
     * <p>通过预处理链处理入站事件。</p>
     *
     * @param client the outbound port
     * @param event  the inbound event
     * @return true when the event is consumed and command dispatch must be skipped
     */
    public boolean process(BotClient client, BotEvent event) {
        PreChain chain = new PreChain(handlers);
        chain.doHandle(client, event);
        return chain.isConsumed();
    }
}
