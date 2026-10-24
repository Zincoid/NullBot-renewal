package com.zincoid.nullbot.bot.cmd.chain;

import com.zincoid.nullbot.bot.cmd.CmdContext;
import com.zincoid.nullbot.bot.spi.event.BotEvent;
import com.zincoid.nullbot.bot.spi.event.GroupEvent;
import com.zincoid.nullbot.bot.spi.event.MsgEvent;
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
        BotEvent event = ctx.event();
        if (event instanceof GroupEvent group) registerGroup(client, group.groupId());
        if (event instanceof MsgEvent msg) registerUser(msg.userId(), msg.nickname());
        else registerUser(client, event.userId());
        chain.doHandle(ctx);
    }

    // ════ Helpers ═════════════════

    private void registerGroup(BotClient client, String groupId) {
        groupService.register(groupId, client.getGroupInfo(groupId).name());
    }

    private void registerUser(BotClient client, String userId) {
        registerUser(userId, client.getUserInfo(userId).nickname());
    }

    private void registerUser(String userId, String nickname) {
        userService.register(userId, nickname);
    }
}
