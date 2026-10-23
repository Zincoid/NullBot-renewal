package com.zincoid.nullbot.bot.spi.port;

/**
 * 机器人账号生命周期端口：接收账号上线与下线回调。
 * <p>Bot account lifecycle port: receives account online and offline callbacks.</p>
 */
public interface BotLifecycle {

    /**
     * 机器人账号上线。
     * <p>The bot account came online.</p>
     *
     * @param selfId the bot account id
     * @param client the outbound port for this account
     */
    void online(String selfId, BotClient client);

    /**
     * 机器人账号下线。
     * <p>The bot account went offline.</p>
     *
     * @param selfId the bot account id
     */
    void offline(String selfId);
}
