package com.zincoid.nullbot.adapter.stdin;

import com.zincoid.nullbot.bot.spi.model.GroupInfo;
import com.zincoid.nullbot.bot.spi.model.MsgContent;
import com.zincoid.nullbot.bot.spi.model.MsgDetail;
import com.zincoid.nullbot.bot.spi.model.UserInfo;
import com.zincoid.nullbot.bot.spi.port.BotClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Stdin 出站适配器：把机器人动作打印到终端，其余伪造。
 * <p>Stdin outbound adapter: prints bot actions to the terminal, fakes the rest.</p>
 */
@Slf4j
@Component
public class StdinClient implements BotClient {

    public static final String SELF_ID = "1";
    public static final String USER_ID = "2";
    public static final String ADMIN_ID = "3";
    public static final String GROUP_ID = "100";

    private final Map<String, MsgDetail> messages = new ConcurrentHashMap<>();
    private final AtomicLong nextId = new AtomicLong(1);

    // ════ Ledger ══════════════════

    String record(String userId, MsgContent content) {
        String id = String.valueOf(nextId.getAndIncrement());
        messages.put(id, new MsgDetail(id, userId, nickname(userId), content));
        return id;
    }

    // ════ Messaging ═══════════════

    @Override
    public String sendGroupMsg(String groupId, MsgContent content) {
        log.info("Send group message to {}: {}", groupId, StdinMapper.toStdin(content));
        return record(SELF_ID, content);
    }

    @Override
    public String sendPrivateMsg(String userId, MsgContent content) {
        log.info("Send private message to {}: {}", userId, StdinMapper.toStdin(content));
        return record(SELF_ID, content);
    }

    @Override
    public MsgDetail getMsg(String messageId) {
        MsgDetail msg = messages.get(messageId);
        if (msg == null) throw new StdinException("Unknown message id: " + messageId);
        return msg;
    }

    @Override
    public void recall(String messageId) {
        MsgDetail msg = messages.remove(messageId);
        if (msg == null) throw new StdinException("Unknown message id: " + messageId);
        log.info("Recall message {}", messageId);
    }

    // ════ Poke ════════════════════

    @Override
    public void pokeGroup(String groupId, String userId) {
        log.info("Poke {} in group {}", userId, groupId);
    }

    @Override
    public void pokePrivate(String userId) {
        log.info("Poke {} in private chat", userId);
    }

    // ════ Mute ════════════════════

    @Override
    public void mute(String groupId, String userId, int seconds) {
        log.info("Mute {} in group {} for {}s", userId, groupId, seconds);
    }

    // ════ Info queries ════════════

    @Override
    public UserInfo getLoginInfo() {
        return new UserInfo(SELF_ID, "stdin-bot", null);
    }

    @Override
    public UserInfo getUserInfo(String userId) {
        return new UserInfo(userId, nickname(userId), null);
    }

    @Override
    public GroupInfo getGroupInfo(String groupId) {
        return new GroupInfo(groupId, "group-" + groupId);
    }

    @Override
    public List<UserInfo> getGroupMembers(String groupId) {
        return List.of(
                getLoginInfo(),
                getUserInfo(USER_ID),
                getUserInfo(ADMIN_ID)
        );
    }

    // ════ Helpers ═════════════════

    private static String nickname(String userId) {
        return SELF_ID.equals(userId) ? "stdin-bot" : "user-" + userId;
    }
}
