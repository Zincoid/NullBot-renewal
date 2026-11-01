package com.zincoid.nullbot.bot.cmd.chain;

import com.zincoid.nullbot.bot.cmd.CmdContext;
import com.zincoid.nullbot.bot.spi.event.GroupEvent;
import com.zincoid.nullbot.bot.spi.event.MsgEvent;
import com.zincoid.nullbot.module.system.CmdUsageService;
import com.zincoid.nullbot.platform.persistence.enums.ChatType;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 指令用量处理器：在指令执行前记录一次指令使用。
 * <p>Command usage handler: records one command usage before command execution.</p>
 */
@Order(6)
@Component
@RequiredArgsConstructor
public class CmdUsageHandler implements CmdHandler {

    private final CmdUsageService cmdUsageService;

    @Override
    public void handle(CmdContext ctx, CmdChain chain) {
        String cmd = ctx.cmd().name();
        MsgEvent event = ctx.event();
        ChatType chatType;
        String chatId;
        if (event instanceof GroupEvent e) {
            chatType = ChatType.GROUP;
            chatId = e.groupId();
        } else {
            chatType = ChatType.PRIVATE;
            chatId = event.userId();
        }
        cmdUsageService.increase(chatType, chatId, event.userId(), cmd);
        chain.doHandle(ctx);
    }
}
