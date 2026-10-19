package com.zincoid.nullbot.adapter.stdin;

import com.zincoid.nullbot.bot.spi.model.MsgContent;
import com.zincoid.nullbot.bot.spi.model.MsgDetail;
import com.zincoid.nullbot.bot.spi.model.UserInfo;
import com.zincoid.nullbot.bot.spi.port.BotClient;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Stdin outbound adapter: prints bot actions to the terminal, fakes the rest.
 * <p>Stdin 出站适配器：把机器人动作打印到终端，其余伪造。</p>
 */
@Slf4j
public class StdinClient implements BotClient {

    public static final long SELF_ID = 1L;
    public static final long USER_ID = 2L;
    public static final long ADMIN_ID = 3L;
    public static final long GROUP_ID = 100L;

    private final Map<Long, MsgDetail> messages = new ConcurrentHashMap<>();
    private final AtomicLong nextId = new AtomicLong(1);

    // ════ Ledger ══════════════════

    long record(long userId, MsgContent content) {
        long id = nextId.getAndIncrement();
        messages.put(id, new MsgDetail(id, userId, content));
        return id;
    }

    // ════ Messaging ═══════════════

    @Override
    public long sendGroupMsg(long groupId, MsgContent content) {
        log.info("Send group message to {}: {}", groupId, StdinMapper.toStdin(content));
        return record(SELF_ID, content);
    }

    @Override
    public long sendPrivateMsg(long userId, MsgContent content) {
        log.info("Send private message to {}: {}", userId, StdinMapper.toStdin(content));
        return record(SELF_ID, content);
    }

    @Override
    public MsgDetail getMsg(long messageId) {
        MsgDetail msg = messages.get(messageId);
        if (msg == null) throw new StdinException("Unknown message id: " + messageId);
        return msg;
    }

    @Override
    public void recall(long messageId) {
        MsgDetail msg = messages.remove(messageId);
        if (msg == null) throw new StdinException("Unknown message id: " + messageId);
        log.info("Recall message {}", messageId);
    }

    // ════ Poke ════════════════════

    @Override
    public void pokeGroup(long groupId, long userId) {
        log.info("Poke {} in group {}", userId, groupId);
    }

    @Override
    public void pokePrivate(long userId) {
        log.info("Poke {} in private chat", userId);
    }

    // ════ Mute ════════════════════

    @Override
    public void mute(long groupId, long userId, int seconds) {
        log.info("Mute {} in group {} for {}s", userId, groupId, seconds);
    }

    // ════ Info queries ════════════

    @Override
    public UserInfo getLoginInfo() {
        return new UserInfo(SELF_ID, "stdin-bot");
    }

    @Override
    public UserInfo getUserInfo(long userId) {
        return new UserInfo(userId, "user-" + userId);
    }
}
