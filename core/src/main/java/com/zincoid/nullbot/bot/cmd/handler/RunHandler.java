package com.zincoid.nullbot.bot.cmd.handler;

import com.zincoid.nullbot.bot.cmd.CmdArgs;
import com.zincoid.nullbot.bot.cmd.CmdContext;
import com.zincoid.nullbot.bot.spi.event.GroupMsgEvent;
import com.zincoid.nullbot.bot.spi.event.PrivateMsgEvent;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 末端处理器：执行命令。
 * <p>Terminal handler: executes the command.</p>
 */
@Order(Ordered.LOWEST_PRECEDENCE)
@Component
public class RunHandler implements CmdHandler {

    // ════ Handle ══════════════════

    @Override
    public void handle(CmdContext ctx, CmdChain chain) {
        CmdArgs args = CmdArgs.of(ctx.params(), ctx.language());
        dispatch(ctx, args);
    }

    // ════ Helpers ═════════════════

    private void dispatch(CmdContext ctx, CmdArgs args) {
        switch (ctx.event()) {
            case GroupMsgEvent e -> ctx.cmd().run(ctx, e, args);
            case PrivateMsgEvent e -> ctx.cmd().run(ctx, e, args);
        }
    }
}
