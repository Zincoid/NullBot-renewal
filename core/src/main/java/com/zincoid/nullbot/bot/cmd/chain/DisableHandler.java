package com.zincoid.nullbot.bot.cmd.chain;

import com.zincoid.nullbot.bot.cmd.CmdContext;
import com.zincoid.nullbot.bot.spi.event.BotEvent;
import com.zincoid.nullbot.bot.spi.event.GroupEvent;
import com.zincoid.nullbot.module.i18n.DurationText;
import com.zincoid.nullbot.module.i18n.Text;
import com.zincoid.nullbot.module.setting.CmdDisableService;
import com.zincoid.nullbot.platform.persistence.po.GroupCmdDisable;
import com.zincoid.nullbot.platform.persistence.po.UserCmdDisable;
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
        Rule rule = effective(ctx.event(), ctx.cmd().name());
        if (rule == null) {
            chain.doHandle(ctx);
            return;
        }
        log.info("Command disabled: cmd={}, user={}, expires={}",
                ctx.cmd().name(), ctx.event().userId(), rule.expiresAt());
        notify(ctx, rule);
    }

    // ════ Effective ═══════════════

    private Rule effective(BotEvent event, String cmd) {
        if (event instanceof GroupEvent group) {
            GroupCmdDisable personal = cmdDisableService.findGroup(group.groupId(), event.userId(), cmd);
            if (personal != null) return new Rule(personal.getUserId(), personal.getExpiresAt());
            GroupCmdDisable whole = cmdDisableService.findGroup(group.groupId(), CmdDisableService.WHOLE_GROUP, cmd);
            return whole == null ? null : new Rule(whole.getUserId(), whole.getExpiresAt());
        }
        UserCmdDisable rule = cmdDisableService.findUser(event.userId(), cmd);
        return rule == null ? null : new Rule(rule.getUserId(), rule.getExpiresAt());
    }

    // ════ Notify ══════════════════

    private void notify(CmdContext ctx, Rule rule) {
        String state = rule.expiresAt() == null
                ? Text.DISABLE_PERMANENT.of(ctx.language())
                : Text.DISABLE_REMAINING.of(ctx.language(),
                        DurationText.format(ctx.language(),
                                Duration.between(LocalDateTime.now(), rule.expiresAt())));
        Text text = CmdDisableService.WHOLE_GROUP.equals(rule.target())
                ? Text.DISABLE_NOTICE_GROUP
                : Text.DISABLE_NOTICE_USER;
        ctx.send(text, state);
    }

    /**
     * 生效规则：用户 ID 为 {@code *} 表示整群。
     * <p>Effective rule: a {@code *} user id means a whole group.</p>
     *
     * @param target    the target user id, {@code *} for a whole group
     * @param expiresAt the expiry time, or null for a permanent rule
     */
    private record Rule(String target, LocalDateTime expiresAt) {
    }
}
