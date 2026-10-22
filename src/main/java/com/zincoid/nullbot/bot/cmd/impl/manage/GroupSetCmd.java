package com.zincoid.nullbot.bot.cmd.impl.manage;

import com.zincoid.nullbot.bot.cmd.Cmd;
import com.zincoid.nullbot.bot.cmd.CmdArgs;
import com.zincoid.nullbot.bot.cmd.CmdContext;
import com.zincoid.nullbot.bot.cmd.CmdMapping;
import com.zincoid.nullbot.bot.exception.CmdWarnException;
import com.zincoid.nullbot.bot.spi.event.GroupMsgEvent;
import com.zincoid.nullbot.module.setting.GroupCmdRateLimiter;
import com.zincoid.nullbot.module.setting.GroupSettingService;
import com.zincoid.nullbot.module.setting.SettingException;
import com.zincoid.nullbot.platform.persistence.enums.Access;
import com.zincoid.nullbot.platform.persistence.enums.Effort;
import com.zincoid.nullbot.platform.persistence.enums.LimitScope;
import com.zincoid.nullbot.platform.persistence.po.GroupSetting;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * 群设置指令：查看或修改当前群的限速与 AI 设置。
 * <p>Group setting command: views or updates the current group's rate-limit and AI settings.</p>
 */
@CmdMapping({"GroupSet", "群设置"})
@Component
@RequiredArgsConstructor
public class GroupSetCmd implements Cmd {

    private final GroupSettingService groupSettingService;
    private final GroupCmdRateLimiter groupCmdRateLimiter;

    // ════ Run ═════════════════════

    @Override
    public void run(CmdContext ctx, GroupMsgEvent event, CmdArgs args) {
        String groupId = event.groupId();
        if (args.hasOpt("limit", "l")) {
            updateLimit(ctx, groupId, args);
            return;
        }
        if (args.hasOpt("ai", "a")) {
            updateAi(ctx, groupId, args);
            return;
        }
        if (args.isEmpty() || "view".equalsIgnoreCase(args.peek())) {
            ctx.send(view(groupId));
            return;
        }
        throw new CmdWarnException("Unknown option");
    }

    // ════ Update ══════════════════

    private void updateLimit(CmdContext ctx, String groupId, CmdArgs args) {
        String name = args.next().toLowerCase(Locale.ROOT);
        GroupSetting update = GroupSetting.builder().groupId(groupId).build();
        String message;
        switch (name) {
            case "enabled" -> {
                boolean enabled = bool(args.next());
                update.setLimitEnabled(enabled);
                message = "limit enabled = " + enabled;
            }
            case "scope" -> {
                LimitScope scope = scope(args.next());
                update.setLimitScope(scope);
                message = "limit scope = " + scope.getLabel();
            }
            case "capacity" -> {
                int value = positive(args.nextInt());
                update.setLimitCapacity(value);
                message = "limit capacity = " + value;
            }
            case "refill" -> {
                int value = positive(args.nextInt());
                update.setLimitRefill(value);
                message = "limit refill = " + value;
            }
            case "interval" -> {
                int value = positive(args.nextInt());
                update.setLimitInterval(value);
                message = "limit interval = " + value;
            }
            default -> throw new CmdWarnException("Unknown limit setting: " + name);
        }
        save(update);
        groupCmdRateLimiter.reset(groupId);
        ctx.send("✅ Group setting updated: " + message);
    }

    private void updateAi(CmdContext ctx, String groupId, CmdArgs args) {
        String name = args.next().toLowerCase(Locale.ROOT);
        GroupSetting update = GroupSetting.builder().groupId(groupId).build();
        String message;
        switch (name) {
            case "effort" -> {
                Effort effort = effort(args.next());
                update.setAiEffort(effort);
                message = "AI effort = " + effort.getLabel();
            }
            case "prompt" -> {
                String text = args.rest("").trim();
                if (text.isEmpty()) throw new CmdWarnException("Missing prompt");
                boolean reset = "reset".equalsIgnoreCase(text);
                update.setAiPrompt(reset ? "" : text);
                message = reset ? "AI prompt reset to default" : "AI prompt updated";
            }
            case "guard" -> {
                boolean enabled = bool(args.next());
                update.setAiInjectionGuard(enabled);
                message = "AI injection guard = " + enabled;
            }
            default -> throw new CmdWarnException("Unknown AI setting: " + name);
        }
        save(update);
        ctx.send("✅ Group setting updated: " + message);
    }

    // ════ View ════════════════════

    private String view(String groupId) {
        GroupSetting setting = groupSettingService.get(groupId);
        LimitScope scope = setting.getLimitScope() == null ? LimitScope.USER : setting.getLimitScope();
        Effort effort = setting.getAiEffort() == null ? Effort.HIGH : setting.getAiEffort();
        String prompt = setting.getAiPrompt() == null || setting.getAiPrompt().isBlank()
                ? "(default)"
                : setting.getAiPrompt();
        return """
                Group settings: %s
                Limit: enabled=%s, scope=%s, capacity=%s, refill=%s, interval=%s
                AI: effort=%s, guard=%s
                Prompt: %s""".formatted(
                groupId,
                setting.getLimitEnabled(), scope.getLabel(),
                setting.getLimitCapacity(), setting.getLimitRefill(), setting.getLimitInterval(),
                effort.getLabel(), setting.getAiInjectionGuard(), prompt);
    }

    // ════ Helpers ═════════════════

    private void save(GroupSetting setting) {
        try {
            groupSettingService.update(setting);
        } catch (SettingException e) {
            throw new CmdWarnException(e.getMessage());
        }
    }

    private static boolean bool(String raw) {
        return switch (raw.toLowerCase(Locale.ROOT)) {
            case "on", "true", "1" -> true;
            case "off", "false", "0" -> false;
            default -> throw new CmdWarnException("Malformed boolean: " + raw);
        };
    }

    private static int positive(int value) {
        if (value <= 0) throw new CmdWarnException("Value must be positive: " + value);
        return value;
    }

    private static LimitScope scope(String raw) {
        for (LimitScope scope : LimitScope.values())
            if (scope.getLabel().equalsIgnoreCase(raw)) return scope;
        throw new CmdWarnException("Unknown scope: " + raw);
    }

    private static Effort effort(String raw) {
        for (Effort effort : Effort.values())
            if (effort.getLabel().equalsIgnoreCase(raw)) return effort;
        throw new CmdWarnException("Unknown effort: " + raw);
    }

    // ════ Cmd ═════════════════════

    @Override
    public Access getAccess() {
        return Access.MANAGER;
    }

    @Override
    public String getHelp() {
        return """
                GroupSet
                Views or updates the current group's settings.
                Usage: GroupSet [-v|--view]
                       GroupSet -l|--limit <enabled|scope|capacity|refill|interval> [value]
                       GroupSet -a|--ai <effort|prompt|guard> [value]
                Values: scope=user|group|cmd, effort=minimal|low|medium|high|xhigh|max|ultra
                Note: "GroupSet ai prompt reset" clears the group prompt.
                Aliases: 群设置
                Access: >=1(manager)""";
    }
}
