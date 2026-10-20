package com.zincoid.nullbot.bot.bus.pre;

import com.zincoid.nullbot.bot.spi.event.BotEvent;
import com.zincoid.nullbot.bot.spi.port.BotClient;

/**
 * One stage of the event pre-processing chain.
 * <p>事件预处理链的一个环节。</p>
 */
public interface PreHandler {

    /**
     * Handles the event, optionally delegating to the remaining chain.
     * <p>处理事件，可按需把事件交给链条后续环节。</p>
     *
     * @param client the outbound port
     * @param event  the inbound event
     * @param chain  the remaining chain
     */
    void handle(BotClient client, BotEvent event, PreChain chain);
}
