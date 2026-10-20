package com.zincoid.nullbot.bot.bus;

import com.zincoid.nullbot.bot.cmd.CmdContext;
import com.zincoid.nullbot.bot.spi.event.GroupMsgEvent;
import com.zincoid.nullbot.bot.spi.event.PrivateMsgEvent;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Outputs command help on {@code --help} / {@code -H}.
 * <p>收到 {@code --help} / {@code -H} 时输出指令帮助。</p>
 */
@Order(1)
@Component
public class HelpHandler implements CmdHandler {

    @Override
    public void handle(CmdContext ctx, CmdChain chain) {
        List<String> params = ctx.params();
        if (!params.isEmpty() && ("--help".equals(params.getFirst()) || "-H".equals(params.getFirst()))) {
            if (ctx.event() instanceof GroupMsgEvent) {
                ctx.send(ctx.cmd().getHelp());
            } else if (ctx.event() instanceof PrivateMsgEvent) {
                ctx.send("⚠️ Help is unavailable in private chat");
            }
            return;
        }
        chain.doHandle(ctx);
    }
}
