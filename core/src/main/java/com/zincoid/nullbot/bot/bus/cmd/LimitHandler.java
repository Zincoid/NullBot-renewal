package com.zincoid.nullbot.bot.bus.cmd;

import com.zincoid.nullbot.bot.cmd.CmdContext;
import com.zincoid.nullbot.bot.spi.event.GroupMsgEvent;
import com.zincoid.nullbot.bot.spi.event.GroupPokeEvent;
import com.zincoid.nullbot.bot.spi.event.GroupRecallEvent;
import com.zincoid.nullbot.bot.spi.event.JoinEvent;
import com.zincoid.nullbot.bot.spi.event.LeaveEvent;
import com.zincoid.nullbot.bot.spi.event.PrivateMsgEvent;
import com.zincoid.nullbot.bot.spi.event.PrivatePokeEvent;
import com.zincoid.nullbot.bot.spi.event.PrivateRecallEvent;
import com.zincoid.nullbot.module.i18n.Text;
import com.zincoid.nullbot.module.setting.GroupCmdRateLimiter;
import com.zincoid.nullbot.module.setting.GroupSettingService;
import com.zincoid.nullbot.platform.persistence.po.GroupSetting;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 限速处理器：在指令执行前按群设置做速率限制。
 * <p>Rate limit handler: limits command execution by group settings.</p>
 */
@Slf4j
@Order(2)
@Component
@RequiredArgsConstructor
public class LimitHandler implements CmdHandler {

    private final GroupSettingService groupSettingService;
    private final GroupCmdRateLimiter groupCmdRateLimiter;

    // ════ Handle ══════════════════

    @Override
    public void handle(CmdContext ctx, CmdChain chain) {
        boolean allowed = switch (ctx.event()) {
            case GroupMsgEvent e -> check(ctx, e.groupId(), e.userId());
            case GroupPokeEvent e -> check(ctx, e.groupId(), e.userId());
            case GroupRecallEvent e -> check(ctx, e.groupId(), e.userId());
            case JoinEvent e -> check(ctx, e.groupId(), e.userId());
            case LeaveEvent e -> check(ctx, e.groupId(), e.userId());
            case PrivateMsgEvent _, PrivatePokeEvent _, PrivateRecallEvent _ -> true;
        };
        if (allowed) chain.doHandle(ctx);
    }

    // ════ Helpers ═════════════════

    private boolean check(CmdContext ctx, String groupId, String userId) {
        GroupSetting setting = groupSettingService.get(groupId);
        if (!Boolean.TRUE.equals(setting.getLimitEnabled())) return true;
        String cmd = ctx.cmd().getClass().getSimpleName().replace("Cmd", "");
        if (groupCmdRateLimiter.tryConsume(groupId, userId, cmd)) return true;
        log.info("Rate limit exceeded: group={}, user={}, cmd={}", groupId, userId, cmd);
        if (ctx.event() instanceof GroupMsgEvent) ctx.send(Text.TOO_MANY_REQUESTS);
        return false;
    }
}
