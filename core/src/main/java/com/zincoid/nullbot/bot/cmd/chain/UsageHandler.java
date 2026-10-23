package com.zincoid.nullbot.bot.cmd.chain;

import com.zincoid.nullbot.bot.cmd.CmdContext;
import com.zincoid.nullbot.bot.spi.event.GroupMsgEvent;
import com.zincoid.nullbot.bot.spi.event.GroupPokeEvent;
import com.zincoid.nullbot.bot.spi.event.GroupRecallEvent;
import com.zincoid.nullbot.bot.spi.event.JoinEvent;
import com.zincoid.nullbot.bot.spi.event.LeaveEvent;
import com.zincoid.nullbot.bot.spi.event.PrivateMsgEvent;
import com.zincoid.nullbot.bot.spi.event.PrivatePokeEvent;
import com.zincoid.nullbot.bot.spi.event.PrivateRecallEvent;
import com.zincoid.nullbot.module.system.UsageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 用量处理器：在指令执行前记录一次指令使用。
 * <p>Usage handler: records one command usage before command execution.</p>
 */
@Order(3)
@Component
@RequiredArgsConstructor
public class UsageHandler implements CmdHandler {

    private final UsageService usageService;

    @Override
    public void handle(CmdContext ctx, CmdChain chain) {
        String cmd = ctx.cmd().getClass().getSimpleName().replace("Cmd", "");
        String groupId;
        String userId;
        switch (ctx.event()) {
            case GroupMsgEvent e -> {
                groupId = e.groupId();
                userId = e.userId();
            }
            case PrivateMsgEvent e -> {
                groupId = "0";
                userId = e.userId();
            }
            case GroupPokeEvent e -> {
                groupId = e.groupId();
                userId = e.userId();
            }
            case PrivatePokeEvent e -> {
                groupId = "0";
                userId = e.userId();
            }
            case GroupRecallEvent e -> {
                groupId = e.groupId();
                userId = e.userId();
            }
            case PrivateRecallEvent e -> {
                groupId = "0";
                userId = e.userId();
            }
            case JoinEvent e -> {
                groupId = e.groupId();
                userId = e.userId();
            }
            case LeaveEvent e -> {
                groupId = e.groupId();
                userId = e.userId();
            }
        }
        usageService.increase(groupId, userId, cmd);
        chain.doHandle(ctx);
    }
}
