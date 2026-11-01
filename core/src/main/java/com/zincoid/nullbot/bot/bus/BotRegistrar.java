package com.zincoid.nullbot.bot.bus;

import com.zincoid.nullbot.bot.BotClients;
import com.zincoid.nullbot.bot.spi.event.BotEvent;
import com.zincoid.nullbot.bot.spi.event.GroupEvent;
import com.zincoid.nullbot.bot.spi.port.BotClient;
import com.zincoid.nullbot.module.common.GroupService;
import com.zincoid.nullbot.module.common.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 机器人实体懒注册：确保事件涉及的群与用户记录存在。
 * <p>Bot entity registrar: ensures the group and user records referenced by an event exist.</p>
 */
@Component
@RequiredArgsConstructor
public class BotRegistrar {

    private final GroupService groupService;
    private final UserService userService;

    // ════ Ensure ══════════════════

    /**
     * 确保事件涉及的群与用户记录存在。
     * <p>Ensures the group and user records referenced by the event exist.</p>
     *
     * @param client the outbound port
     * @param event  the inbound event
     */
    public void ensure(BotClient client, BotEvent event) {
        if (event == null || client == null) return;
        if (event instanceof GroupEvent e)
            groupService.register(e.groupId(), BotClients.groupName(client, e.groupId()));
        userService.register(event.userId(), BotClients.userName(client, event.userId()));
    }
}
