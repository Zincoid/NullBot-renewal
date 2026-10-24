package com.zincoid.nullbot.bot.cmd.chain;

import com.zincoid.nullbot.bot.cmd.CmdArgs;
import com.zincoid.nullbot.bot.cmd.CmdContext;
import com.zincoid.nullbot.bot.cmd.CmdInfoException;
import com.zincoid.nullbot.bot.cmd.CmdWarnException;
import com.zincoid.nullbot.bot.spi.event.GroupMsgEvent;
import com.zincoid.nullbot.bot.spi.event.GroupPokeEvent;
import com.zincoid.nullbot.bot.spi.event.GroupRecallEvent;
import com.zincoid.nullbot.bot.spi.event.JoinEvent;
import com.zincoid.nullbot.bot.spi.event.LeaveEvent;
import com.zincoid.nullbot.bot.spi.event.PrivateMsgEvent;
import com.zincoid.nullbot.bot.spi.event.PrivatePokeEvent;
import com.zincoid.nullbot.bot.spi.event.PrivateRecallEvent;
import com.zincoid.nullbot.module.i18n.Text;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 末端处理器：执行命令，并将流程控制异常转换为用户回复。
 * <p>Terminal handler: executes the command and converts flow-control exceptions into replies.</p>
 */
@Slf4j
@Order(Ordered.LOWEST_PRECEDENCE)
@Component
public class RunHandler implements CmdHandler {

    // ════ Handle ══════════════════

    @Override
    public void handle(CmdContext ctx, CmdChain chain) {
        CmdArgs args = CmdArgs.of(ctx.params(), ctx.language());
        try {
            dispatch(ctx, args);
        } catch (CmdInfoException e) {
            log.info("Cmd info: {}", e.getMessage());
            ctx.send(e.getMessage());
        } catch (CmdWarnException e) {
            log.warn("Cmd warning: {}", e.getMessage());
            ctx.send(Text.CMD_WARN, e.getMessage());
        } catch (Exception e) {
            log.error("Cmd fatal: {}", e.getMessage());
            ctx.send(Text.CMD_ERROR, e.getMessage());
            throw e;
        }
    }

    // ════ Helpers ═════════════════

    private void dispatch(CmdContext ctx, CmdArgs args) {
        switch (ctx.event()) {
            case GroupMsgEvent e -> ctx.cmd().run(ctx, e, args);
            case PrivateMsgEvent e -> ctx.cmd().run(ctx, e, args);
            case GroupPokeEvent e -> ctx.cmd().run(ctx, e, args);
            case PrivatePokeEvent e -> ctx.cmd().run(ctx, e, args);
            case GroupRecallEvent _, PrivateRecallEvent _, JoinEvent _, LeaveEvent _ -> {
            }
        }
    }
}
