package com.zincoid.nullbot.adapter.stdin;

import com.zincoid.nullbot.bot.spi.port.BotLifecycle;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Stdin 生命周期监听器：终端适配器启动时上线，关闭时下线。
 * <p>Stdin lifecycle listener: goes online when the terminal adapter starts and offline on shutdown.</p>
 */
@Component
@ConditionalOnProperty(name = "bot.adapter", havingValue = "stdin")
@RequiredArgsConstructor
public class StdinLifecycleListener {

    private final BotLifecycle lifecycle;
    private final StdinClient client = new StdinClient();

    @PostConstruct
    void start() {
        lifecycle.online(StdinClient.SELF_ID, client);
    }

    @PreDestroy
    void stop() {
        lifecycle.offline(StdinClient.SELF_ID);
    }
}
