package com.zincoid.nullbot.bot.cmd.chain;

import com.zincoid.nullbot.bot.cmd.CmdContext;
import com.zincoid.nullbot.bot.spi.event.GroupMsgEvent;
import com.zincoid.nullbot.bot.spi.event.PrivateMsgEvent;
import com.zincoid.nullbot.module.i18n.Text;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 收到 {@code --help} / {@code -H} 时输出指令帮助。
 * <p>Outputs command help on {@code --help} / {@code -H}.</p>
 */
@Order(2)
@Component
public class HelpHandler implements CmdHandler {

    @Override
    public void handle(CmdContext ctx, CmdChain chain) {
        List<String> params = ctx.params();
        if (!params.isEmpty() && ("--help".equals(params.getFirst()) || "-H".equals(params.getFirst()))) {
            if (ctx.event() instanceof GroupMsgEvent) {
                ctx.send(ctx.cmd().getHelp(ctx.language()));
            } else if (ctx.event() instanceof PrivateMsgEvent) {
                ctx.send(Text.HELP_PRIVATE_UNAVAILABLE);
            }
            return;
        }
        chain.doHandle(ctx);
    }
}
