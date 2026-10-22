package com.zincoid.nullbot.bot.spi.port;

import com.zincoid.nullbot.bot.spi.event.BotEvent;

/**
 * 入站端口：适配器把归一化事件交给业务代码。
 * <p>Inbound port: adapters deliver normalized events to business code.</p>
 */
public interface BotServer {

    void handle(BotClient client, BotEvent event);
}
