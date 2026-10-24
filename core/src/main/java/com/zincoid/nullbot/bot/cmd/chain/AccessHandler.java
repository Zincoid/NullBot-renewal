package com.zincoid.nullbot.bot.cmd.chain;

import com.zincoid.nullbot.bot.cmd.CmdContext;
import com.zincoid.nullbot.bot.spi.event.BotEvent;
import com.zincoid.nullbot.bot.spi.event.GroupEvent;
import com.zincoid.nullbot.module.common.GroupService;
import com.zincoid.nullbot.module.common.UserService;
import com.zincoid.nullbot.module.i18n.Text;
import com.zincoid.nullbot.platform.persistence.enums.Access;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 权限处理器：在指令执行前校验群与用户的限权等级，并拦截不允许私聊的用户。
 * <p>Access handler: checks group and user access levels and rejects private chat from disallowed users.</p>
 */
@Slf4j
@Order(0)
@Component
@RequiredArgsConstructor
public class AccessHandler implements CmdHandler {

    private final GroupService groupService;
    private final UserService userService;

    // ════ Handle ══════════════════

    @Override
    public void handle(CmdContext ctx, CmdChain chain) {
        if (!ctx.checkAccess()) {
            chain.doHandle(ctx);
            return;
        }
        Access required = ctx.cmd().getAccess();
        BotEvent event = ctx.event();
        boolean allowed = event instanceof GroupEvent group
                ? checkGroup(ctx, required, group.groupId(), event.userId())
                : checkPrivate(ctx, required, event.userId());
        if (allowed) chain.doHandle(ctx);
    }

    // ════ Helpers ═════════════════

    private boolean checkPrivate(CmdContext ctx, Access required, String userId) {
        if (!userService.isPrivateChatAllowed(userId)) {
            log.info("Private chat denied: user={}", userId);
            return false;
        }
        return checkUser(ctx, required, userId);
    }

    private boolean checkGroup(CmdContext ctx, Access required, String groupId, String userId) {
        Access groupAccess = groupService.getAccess(groupId);
        if (groupAccess.getValue() < required.getValue()) {
            log.info("Group access denied: required={}, actual={}", required.getLabel(), groupAccess.getLabel());
            return false;
        }
        return checkUser(ctx, required, userId);
    }

    private boolean checkUser(CmdContext ctx, Access required, String userId) {
        Access userAccess = userService.getAccess(userId);
        if (userAccess.getValue() < required.getValue()) {
            log.info("User access denied: required={}, actual={}", required.getLabel(), userAccess.getLabel());
            ctx.send(Text.ACCESS_DENIED, required.getLabel(), userAccess.getLabel());
            return false;
        }
        return true;
    }
}
