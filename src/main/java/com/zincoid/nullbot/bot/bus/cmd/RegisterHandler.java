package com.zincoid.nullbot.bot.bus.cmd;

import com.zincoid.nullbot.bot.cmd.CmdContext;
import com.zincoid.nullbot.bot.spi.event.GroupMsgEvent;
import com.zincoid.nullbot.bot.spi.event.GroupPokeEvent;
import com.zincoid.nullbot.bot.spi.event.GroupRecallEvent;
import com.zincoid.nullbot.bot.spi.event.JoinEvent;
import com.zincoid.nullbot.bot.spi.event.LeaveEvent;
import com.zincoid.nullbot.bot.spi.event.PrivateMsgEvent;
import com.zincoid.nullbot.bot.spi.event.PrivatePokeEvent;
import com.zincoid.nullbot.bot.spi.event.PrivateRecallEvent;
import com.zincoid.nullbot.bot.spi.port.BotClient;
import com.zincoid.nullbot.module.common.GroupService;
import com.zincoid.nullbot.module.common.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 注册处理器：在指令执行前刷新群与用户的名称记录。
 * <p>Registration handler: refreshes group and user name records before command execution.</p>
 */
@Order(-1)
@Component
@RequiredArgsConstructor
public class RegisterHandler implements CmdHandler {

    private final GroupService groupService;
    private final UserService userService;

    // ════ Handle ══════════════════

    @Override
    public void handle(CmdContext ctx, CmdChain chain) {
        BotClient client = ctx.client();
        switch (ctx.event()) {
            case GroupMsgEvent e -> {
                registerGroup(client, e.groupId());
                registerUser(e.userId(), e.nickname());
            }
            case PrivateMsgEvent e -> registerUser(e.userId(), e.nickname());
            case GroupPokeEvent e -> {
                registerGroup(client, e.groupId());
                registerUser(client, e.userId());
            }
            case PrivatePokeEvent e -> registerUser(client, e.userId());
            case GroupRecallEvent e -> {
                registerGroup(client, e.groupId());
                registerUser(client, e.userId());
            }
            case PrivateRecallEvent e -> registerUser(client, e.userId());
            case JoinEvent e -> {
                registerGroup(client, e.groupId());
                registerUser(client, e.userId());
            }
            case LeaveEvent e -> {
                registerGroup(client, e.groupId());
                registerUser(client, e.userId());
            }
        }
        chain.doHandle(ctx);
    }

    // ════ Helpers ═════════════════

    private void registerGroup(BotClient client, String groupId) {
        groupService.register(groupId, client.getGroupInfo(groupId).groupName());
    }

    private void registerUser(BotClient client, String userId) {
        registerUser(userId, client.getUserInfo(userId).nickname());
    }

    private void registerUser(String userId, String nickname) {
        userService.register(userId, nickname);
    }
}
