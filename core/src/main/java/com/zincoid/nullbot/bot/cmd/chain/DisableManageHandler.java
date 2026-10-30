package com.zincoid.nullbot.bot.cmd.chain;

import com.zincoid.nullbot.bot.cmd.CmdArgs;
import com.zincoid.nullbot.bot.cmd.CmdContext;
import com.zincoid.nullbot.bot.cmd.CmdWarnException;
import com.zincoid.nullbot.bot.spi.event.GroupEvent;
import com.zincoid.nullbot.bot.spi.event.MsgEvent;
import com.zincoid.nullbot.module.common.UserService;
import com.zincoid.nullbot.module.i18n.DurationText;
import com.zincoid.nullbot.module.i18n.Text;
import com.zincoid.nullbot.module.setting.CmdDisableService;
import com.zincoid.nullbot.platform.persistence.enums.Access;
import com.zincoid.nullbot.platform.util.DurationUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.DateTimeException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 禁用管理处理器：处理 {@code -B} / {@code -U} 管理请求。
 * <p>Disable management handler: handles {@code -B} / {@code -U} requests.</p>
 */
@Slf4j
@Order(3)
@Component
@RequiredArgsConstructor
public class DisableManageHandler implements CmdHandler {

    private final CmdDisableService cmdDisableService;
    private final UserService userService;

    // ════ Handle ══════════════════

    @Override
    public void handle(CmdContext ctx, CmdChain chain) {
        CmdArgs args = CmdArgs.of(ctx.params(), ctx.language());
        boolean ban = args.hasOpt("ban", "B");
        boolean unban = args.hasOpt("unban", "U");
        if (!ban && !unban) {
            chain.doHandle(ctx);
            return;
        }
        if (ban && unban)
            throw new CmdWarnException(ctx.render(Text.DISABLE_CONFLICT));
        if (!args.isEmpty())
            throw new CmdWarnException(ctx.render(Text.DISABLE_UNEXPECTED_ARG, args.peek()));
        Target target = target(ctx, args);
        if (!checkPermission(ctx, target)) return;
        String cmd = ctx.cmd().name();
        if (unban) removeRule(ctx, args, target, cmd);
        else addRule(ctx, args, target, cmd);
    }

    // ════ Rules ═══════════════════

    private void addRule(CmdContext ctx, CmdArgs args, Target target, String cmd) {
        LocalDateTime expiresAt = expiresAt(ctx, args);
        LocalDateTime storedExpiry;
        if (target.groupRule()) {
            storedExpiry = cmdDisableService.addGroup(
                    target.groupId(), target.userId(), cmd, expiresAt).getExpiresAt();
        } else {
            storedExpiry = cmdDisableService.addUser(
                    target.userId(), cmd, expiresAt).getExpiresAt();
        }
        String state = storedExpiry == null
                ? Text.DISABLE_PERMANENT.of(ctx.language())
                : Text.DISABLE_REMAINING.of(ctx.language(),
                        DurationText.format(ctx.language(),
                                Duration.between(LocalDateTime.now(), storedExpiry)));
        ctx.send(Text.DISABLE_ADDED, cmd, targetLabel(ctx, target), state);
    }

    private void removeRule(CmdContext ctx, CmdArgs args, Target target, String cmd) {
        if (args.hasOpt("temp", null))
            throw new CmdWarnException(ctx.render(Text.DISABLE_TEMP_WITH_UNBAN));
        boolean removed = target.groupRule()
                ? cmdDisableService.removeGroup(target.groupId(), target.userId(), cmd)
                : cmdDisableService.removeUser(target.userId(), cmd);
        if (!removed)
            throw new CmdWarnException(ctx.render(Text.DISABLE_NOT_FOUND));
        ctx.send(Text.DISABLE_REMOVED, cmd, targetLabel(ctx, target));
    }

    // ════ Target ══════════════════

    private Target target(CmdContext ctx, CmdArgs args) {
        String user = userOption(ctx, args);
        if (ctx.event() instanceof GroupEvent group) {
            List<String> mentions = mentions(ctx);
            if (mentions.size() > 1 || (user != null && !mentions.isEmpty()))
                throw new CmdWarnException(ctx.render(Text.DISABLE_MULTIPLE_TARGETS));
            String userId = user;
            if (userId == null)
                userId = mentions.isEmpty() ? CmdDisableService.WHOLE_GROUP : mentions.getFirst();
            return new Target(true, group.groupId(), userId);
        }
        if (user == null)
            throw new CmdWarnException(ctx.render(Text.DISABLE_MISSING_USER));
        return new Target(false, "", user);
    }

    private static String userOption(CmdContext ctx, CmdArgs args) {
        String value = args.getOpt("user", null, null);
        if (value == null) return null;
        if ("true".equals(value) || value.isBlank())
            throw new CmdWarnException(ctx.render(Text.DISABLE_EMPTY_USER));
        return value.trim();
    }

    private static List<String> mentions(CmdContext ctx) {
        if (!(ctx.event() instanceof MsgEvent msg)) return List.of();
        return msg.content().atIds().stream()
                .filter(id -> id != null && !id.isBlank() && !id.equals(ctx.event().selfId()))
                .toList();
    }

    // ════ Permission ══════════════

    private boolean checkPermission(CmdContext ctx, Target target) {
        Access required = target.groupRule() ? Access.MANAGER : Access.ADMIN;
        Access actual = userService.getAccess(ctx.event().userId());
        if (actual.getValue() < required.getValue()) {
            log.info("Command disable user denied: user={}, required={}, actual={}",
                    ctx.event().userId(), required.getLabel(), actual.getLabel());
            ctx.send(Text.ACCESS_DENIED, required.getValue(), actual.getValue());
            return false;
        }
        return true;
    }

    // ════ Expiry ══════════════════

    private static LocalDateTime expiresAt(CmdContext ctx, CmdArgs args) {
        String raw = args.getOpt("temp", null, null);
        if (raw == null) return null;
        Duration duration = DurationUtils.parse(raw);
        if (duration == null)
            throw new CmdWarnException(ctx.render(Text.DISABLE_MALFORMED_TEMP, raw));
        try {
            return LocalDateTime.now().plus(duration);
        } catch (DateTimeException e) {
            throw new CmdWarnException(ctx.render(Text.DISABLE_MALFORMED_TEMP, raw));
        }
    }

    private static String targetLabel(CmdContext ctx, Target target) {
        return CmdDisableService.WHOLE_GROUP.equals(target.userId())
                ? ctx.render(Text.DISABLE_TARGET_GROUP)
                : ctx.render(Text.DISABLE_TARGET_USER, target.userId());
    }

    /**
     * 管理目标：群聊或私聊、群/用户 ID。
     * <p>Management target: group or private chat, and group/user ids.</p>
     *
     * @param groupRule whether the target is a group rule
     * @param groupId   the group id, empty for private rules
     * @param userId    the user id, {@code *} for a whole group
     */
    private record Target(boolean groupRule, String groupId, String userId) {
    }
}
