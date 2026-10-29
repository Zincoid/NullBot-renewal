package com.zincoid.nullbot.bot.cmd.chain;

import com.zincoid.nullbot.bot.cmd.CmdContext;
import com.zincoid.nullbot.bot.spi.event.BotEvent;
import com.zincoid.nullbot.bot.spi.event.GroupEvent;
import com.zincoid.nullbot.bot.spi.event.GroupMsgEvent;
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
@Order(5)
@Component
@RequiredArgsConstructor
public class LimitHandler implements CmdHandler {

    private final GroupSettingService groupSettingService;
    private final GroupCmdRateLimiter groupCmdRateLimiter;

    // ════ Handle ══════════════════

    @Override
    public void handle(CmdContext ctx, CmdChain chain) {
        BotEvent event = ctx.event();
        if (event instanceof GroupEvent group && !check(ctx, group.groupId(), event.userId())) return;
        chain.doHandle(ctx);
    }

    // ════ Helpers ═════════════════

    private boolean check(CmdContext ctx, String groupId, String userId) {
        GroupSetting setting = groupSettingService.get(groupId);
        if (!setting.getLimitEnabled()) return true;
        String cmd = ctx.cmd().name();
        if (groupCmdRateLimiter.tryConsume(groupId, userId, cmd)) return true;
        log.info("Rate limit exceeded: group={}, user={}, cmd={}", groupId, userId, cmd);
        if (ctx.event() instanceof GroupMsgEvent) ctx.send(Text.TOO_MANY_REQUESTS);
        return false;
    }
}
