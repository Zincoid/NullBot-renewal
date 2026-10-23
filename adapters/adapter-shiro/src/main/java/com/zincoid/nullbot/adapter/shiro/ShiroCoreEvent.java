package com.zincoid.nullbot.adapter.shiro;

import com.mikuac.shiro.core.Bot;
import com.mikuac.shiro.core.CoreEvent;
import com.zincoid.nullbot.bot.spi.port.BotLifecycle;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 用账号生命周期回调替换 Shiro 默认的 {@link CoreEvent}。
 * <p>Replaces the default Shiro {@link CoreEvent} with account lifecycle callbacks.</p>
 */
@Slf4j
@Primary
@Component
@ConditionalOnProperty(name = "bot.adapter", havingValue = "shiro", matchIfMissing = true)
@RequiredArgsConstructor
public class ShiroCoreEvent extends CoreEvent {

    private final List<BotLifecycle> lifecycles;

    /**
     * 转发账号上线事件。
     * <p>Forwards the account online event.</p>
     *
     * @param bot the connected bot
     */
    @Override
    public void online(Bot bot) {
        String selfId = String.valueOf(bot.getSelfId());
        for (BotLifecycle lifecycle : lifecycles) {
            try {
                lifecycle.online(selfId);
            } catch (RuntimeException e) {
                log.warn("Bot lifecycle online failed: {}", selfId, e);
            }
        }
    }

    /**
     * 转发账号下线事件。
     * <p>Forwards the account offline event.</p>
     *
     * @param account the disconnected account id
     */
    @Override
    public void offline(long account) {
        String selfId = String.valueOf(account);
        for (BotLifecycle lifecycle : lifecycles) {
            try {
                lifecycle.offline(selfId);
            } catch (RuntimeException e) {
                log.warn("Bot lifecycle offline failed: {}", selfId, e);
            }
        }
    }
}
