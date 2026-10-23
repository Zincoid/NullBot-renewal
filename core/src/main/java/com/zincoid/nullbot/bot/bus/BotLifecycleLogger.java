package com.zincoid.nullbot.bot.bus;

import com.zincoid.nullbot.bot.spi.port.BotLifecycle;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 记录机器人账号的上线与下线。
 * <p>Logs bot account online and offline transitions.</p>
 */
@Slf4j
@Component
public class BotLifecycleLogger implements BotLifecycle {

    /**
     * 记录账号上线。
     * <p>Logs an account coming online.</p>
     *
     * @param selfId the bot account id
     */
    @Override
    public void online(String selfId) {
        log.info("Bot online: {}", selfId);
    }

    /**
     * 记录账号下线。
     * <p>Logs an account going offline.</p>
     *
     * @param selfId the bot account id
     */
    @Override
    public void offline(String selfId) {
        log.info("Bot offline: {}", selfId);
    }
}
