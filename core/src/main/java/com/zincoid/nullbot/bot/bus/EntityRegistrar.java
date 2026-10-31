package com.zincoid.nullbot.bot.bus;

import com.zincoid.nullbot.bot.BotClients;
import com.zincoid.nullbot.bot.spi.event.BotEvent;
import com.zincoid.nullbot.bot.spi.event.GroupEvent;
import com.zincoid.nullbot.bot.spi.event.MsgEvent;
import com.zincoid.nullbot.bot.spi.event.PokeEvent;
import com.zincoid.nullbot.bot.spi.port.BotClient;
import com.zincoid.nullbot.module.common.GroupService;
import com.zincoid.nullbot.module.common.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 事件实体懒注册：确保事件涉及的群与用户记录存在。
 * <p>Lazy entity registration: ensures the group and user records referenced by an event exist.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EntityRegistrar {

    private final GroupService groupService;
    private final UserService userService;

    // ════ Ensure ══════════════════

    /**
     * 按事件类型确保群与用户记录存在。
     * <p>Ensures the group and user records exist according to the event type.</p>
     *
     * @param client the outbound port
     * @param event  the inbound event
     */
    public void ensure(BotClient client, BotEvent event) {
        if (event == null || client == null) return;
        if (event instanceof GroupEvent group)
            groupService.register(group.groupId(), groupName(client, group.groupId()));
        if (event instanceof MsgEvent msg) {
            userService.register(msg.userId(), displayName(msg.userId(), msg.nickname()));
        } else if (event instanceof PokeEvent poke) {
            userService.register(poke.userId(), BotClients.nickname(client, poke.userId()));
        }
    }

    // ════ Helpers ═════════════════

    private static String groupName(BotClient client, String groupId) {
        try {
            return displayName(groupId, client.getGroupInfo(groupId).name());
        } catch (RuntimeException e) {
            log.warn("Failed to query group info: {}", groupId, e);
            return groupId;
        }
    }

    private static String displayName(String id, String name) {
        return name == null || name.isBlank() ? id : name;
    }
}
