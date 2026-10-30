package com.zincoid.nullbot.bot.cmd.chain;

import com.zincoid.nullbot.bot.cmd.CmdContext;
import com.zincoid.nullbot.bot.spi.event.BotEvent;
import com.zincoid.nullbot.bot.spi.event.GroupEvent;
import com.zincoid.nullbot.module.i18n.DurationText;
import com.zincoid.nullbot.module.i18n.Text;
import com.zincoid.nullbot.module.setting.CmdDisableService;
import com.zincoid.nullbot.platform.persistence.po.CmdDisable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 禁用拦截处理器：在执行前拦截被禁用的指令。
 * <p>Disable handler: blocks disabled commands before execution.</p>
 */
@Slf4j
@Order(4)
@Component
@RequiredArgsConstructor
public class DisableHandler implements CmdHandler {

    private final CmdDisableService cmdDisableService;

    // ════ Handle ══════════════════

    @Override
    public void handle(CmdContext ctx, CmdChain chain) {
        CmdDisable rule = effective(ctx.event(), ctx.cmd().name());
        if (rule == null) {
            chain.doHandle(ctx);
            return;
        }
        log.info("Command disabled: cmd={}, user={}, expires={}",
                ctx.cmd().name(), ctx.event().userId(), rule.getExpiresAt());
        notify(ctx, rule);
    }

    // ════ Effective ═══════════════

    private CmdDisable effective(BotEvent event, String cmd) {
        if (event instanceof GroupEvent group) {
            CmdDisable personal = cmdDisableService.findGroup(group.groupId(), event.userId(), cmd);
            if (personal != null) return personal;
            return cmdDisableService.findGroup(group.groupId(), CmdDisableService.WHOLE_GROUP, cmd);
        }
        return cmdDisableService.findPrivate(event.userId(), cmd);
    }

    // ════ Notify ══════════════════

    private void notify(CmdContext ctx, CmdDisable rule) {
        String state = rule.getExpiresAt() == null
                ? Text.DISABLE_PERMANENT.of(ctx.language())
                : Text.DISABLE_REMAINING.of(ctx.language(),
                        DurationText.format(ctx.language(),
                                Duration.between(LocalDateTime.now(), rule.getExpiresAt())));
        Text text = CmdDisableService.WHOLE_GROUP.equals(rule.getUserId())
                ? Text.DISABLE_NOTICE_GROUP
                : Text.DISABLE_NOTICE_USER;
        ctx.send(text, state);
    }
}
