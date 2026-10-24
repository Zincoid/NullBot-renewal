package com.zincoid.nullbot.bot.cmd.chain;

import com.zincoid.nullbot.bot.cmd.CmdContext;
import com.zincoid.nullbot.bot.spi.event.BotEvent;
import com.zincoid.nullbot.bot.spi.event.GroupEvent;
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
        String cmd = ctx.cmd().name();
        BotEvent event = ctx.event();
        String groupId = event instanceof GroupEvent group ? group.groupId() : "0";
        usageService.increase(groupId, event.userId(), cmd);
        chain.doHandle(ctx);
    }
}
