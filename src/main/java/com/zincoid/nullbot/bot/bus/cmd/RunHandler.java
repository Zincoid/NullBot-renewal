package com.zincoid.nullbot.bot.bus.cmd;

import com.zincoid.nullbot.bot.cmd.CmdArgs;
import com.zincoid.nullbot.bot.cmd.CmdContext;
import com.zincoid.nullbot.bot.exception.CmdErrorException;
import com.zincoid.nullbot.bot.exception.CmdInfoException;
import com.zincoid.nullbot.bot.exception.CmdOmitException;
import com.zincoid.nullbot.bot.exception.CmdWarnException;
import com.zincoid.nullbot.bot.spi.event.GroupMsgEvent;
import com.zincoid.nullbot.bot.spi.event.GroupPokeEvent;
import com.zincoid.nullbot.bot.spi.event.GroupRecallEvent;
import com.zincoid.nullbot.bot.spi.event.JoinEvent;
import com.zincoid.nullbot.bot.spi.event.LeaveEvent;
import com.zincoid.nullbot.bot.spi.event.PrivateMsgEvent;
import com.zincoid.nullbot.bot.spi.event.PrivatePokeEvent;
import com.zincoid.nullbot.bot.spi.event.PrivateRecallEvent;
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
        CmdArgs args = CmdArgs.of(ctx.params());
        try {
            dispatch(ctx, args);
        } catch (CmdOmitException e) {
            log.info("Command omitted: {}", e.getMessage());
            // this exception has no notification
        } catch (CmdInfoException e) {
            log.info("Command info: {}", e.getMessage());
            ctx.send(e.getMessage());
        } catch (CmdWarnException e) {
            log.warn("Command warning: {}", e.getMessage());
            ctx.send("⚠️ Warn: " + e.getMessage());
        } catch (CmdErrorException e) {
            log.error("Command error: {}", e.getMessage());
            ctx.send("❌ Error: " + e.getMessage());
        } catch (Exception e) {
            log.error("Command fatal: {}", e.getMessage());
            ctx.send("❌ Fatal: " + e.getMessage());
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
            case GroupRecallEvent e -> ctx.cmd().run(ctx, e, args);
            case PrivateRecallEvent e -> ctx.cmd().run(ctx, e, args);
            case JoinEvent e -> ctx.cmd().run(ctx, e, args);
            case LeaveEvent e -> ctx.cmd().run(ctx, e, args);
        }
    }
}
