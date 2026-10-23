package com.zincoid.nullbot.adapter.shiro;

import com.mikuac.shiro.core.Bot;
import com.mikuac.shiro.core.CoreEvent;
import com.zincoid.nullbot.bot.spi.port.BotLifecycle;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

/**
 * 账号生命周期监听器：替换 Shiro 默认的 {@link CoreEvent} 并转发上下线。
 * <p>Account lifecycle listener: replaces Shiro's default {@link CoreEvent} and forwards online and offline events.</p>
 */
@Slf4j
@Primary
@Component
@ConditionalOnProperty(name = "bot.adapter", havingValue = "shiro", matchIfMissing = true)
@RequiredArgsConstructor
public class ShiroLifecycleListener extends CoreEvent {

    private final BotLifecycle lifecycle;

    @Override
    public void online(Bot bot) {
        String selfId = String.valueOf(bot.getSelfId());
        WebSocketSession session = bot.getSession();
        if (session == null || !session.isOpen()) {
            log.warn("Bot lifecycle online skipped: closed session for {}", selfId);
            return;
        }
        try {
            lifecycle.online(selfId, new ShiroClient(bot));
        } catch (RuntimeException e) {
            log.warn("Bot lifecycle online failed: {}", selfId, e);
        }
    }

    @Override
    public void offline(long account) {
        String selfId = String.valueOf(account);
        try {
            lifecycle.offline(selfId);
        } catch (RuntimeException e) {
            log.warn("Bot lifecycle offline failed: {}", selfId, e);
        }
    }
}
