package com.zincoid.nullbot.bot.bus;

import com.zincoid.nullbot.bot.spi.port.BotClient;
import com.zincoid.nullbot.bot.spi.port.BotLifecycle;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 机器人账号生命周期跟踪器：只维护一个在线账号。
 * <p>Bot account lifecycle tracker: keeps only one online account.</p>
 */
@Slf4j
@Component
public class BotLifecycleTracker implements BotLifecycle {

    private Entry entry;

    /**
     * 记录账号上线；已有在线账号时拒绝新的账号。
     * <p>Logs an account coming online and rejects another account while one is online.</p>
     *
     * @param selfId the bot account id
     * @param client the outbound port for this account
     * @throws BusException when another account is already online
     */
    @Override
    public synchronized void online(String selfId, BotClient client) {
        if (entry != null)
            throw new BusException("Bot already online: " + entry.selfId());
        entry = new Entry(selfId, client);
        log.info("Bot online: {}", selfId);
    }

    /**
     * 记录账号下线并清空在线账号。
     * <p>Logs an account going offline and clears the online account.</p>
     *
     * @param selfId the bot account id
     */
    @Override
    public synchronized void offline(String selfId) {
        if (entry == null) {
            log.warn("Bot offline ignored: {}", selfId);
            return;
        }
        if (!entry.selfId().equals(selfId)) {
            log.warn("Bot offline ignored: expected={}, actual={}", entry.selfId(), selfId);
            return;
        }
        entry = null;
        log.info("Bot offline: {}", selfId);
    }

    /**
     * 获取当前在线账号的出站端口。
     * <p>Returns the outbound port of the online account.</p>
     *
     * @return the outbound port
     * @throws BusException when no account is online
     */
    public synchronized BotClient getBotClient() {
        if (entry == null) throw new BusException("Bot offline");
        return entry.client();
    }

    /**
     * 当前是否有账号在线。
     * <p>Whether an account is currently online.</p>
     *
     * @return true when an account is online
     */
    public synchronized boolean isOnline() {
        return entry != null;
    }

    /**
     * 当前在线账号 ID。
     * <p>The id of the online account.</p>
     *
     * @return the online account id, or null when offline
     */
    public synchronized String onlineId() {
        return entry == null ? null : entry.selfId();
    }

    private record Entry(String selfId, BotClient client) {
    }
}
