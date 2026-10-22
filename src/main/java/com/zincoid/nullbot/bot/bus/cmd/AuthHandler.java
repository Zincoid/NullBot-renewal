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
import com.zincoid.nullbot.module.common.GroupService;
import com.zincoid.nullbot.module.common.UserService;
import com.zincoid.nullbot.module.i18n.Text;
import com.zincoid.nullbot.platform.persistence.enums.Access;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 鉴权处理器：在指令执行前校验群与用户的限权等级。
 * <p>Authorization handler: checks group and user access levels before command execution.</p>
 */
@Slf4j
@Order(0)
@Component
@RequiredArgsConstructor
public class AuthHandler implements CmdHandler {

    private final GroupService groupService;
    private final UserService userService;

    // ════ Handle ══════════════════

    @Override
    public void handle(CmdContext ctx, CmdChain chain) {
        Access required = ctx.cmd().getAccess();
        boolean allowed = switch (ctx.event()) {
            case GroupMsgEvent e -> checkGroup(ctx, required, e.groupId(), e.userId());
            case PrivateMsgEvent e -> checkUser(ctx, required, e.userId());
            case GroupPokeEvent e -> checkGroup(ctx, required, e.groupId(), e.userId());
            case PrivatePokeEvent e -> checkUser(ctx, required, e.userId());
            case GroupRecallEvent e -> checkGroup(ctx, required, e.groupId(), e.userId());
            case PrivateRecallEvent e -> checkUser(ctx, required, e.userId());
            case JoinEvent e -> checkGroup(ctx, required, e.groupId(), e.userId());
            case LeaveEvent e -> checkGroup(ctx, required, e.groupId(), e.userId());
        };
        if (allowed) chain.doHandle(ctx);
    }

    // ════ Helpers ═════════════════

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
