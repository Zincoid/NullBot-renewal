package com.zincoid.nullbot.bot.cmd.impl.manage;

import com.zincoid.nullbot.bot.cmd.Cmd;
import com.zincoid.nullbot.bot.cmd.CmdArgs;
import com.zincoid.nullbot.bot.cmd.CmdContext;
import com.zincoid.nullbot.bot.cmd.CmdMapping;
import com.zincoid.nullbot.bot.exception.CmdWarnException;
import com.zincoid.nullbot.bot.spi.event.GroupMsgEvent;
import com.zincoid.nullbot.module.i18n.LanguageService;
import com.zincoid.nullbot.module.i18n.Text;
import com.zincoid.nullbot.module.setting.GroupCmdRateLimiter;
import com.zincoid.nullbot.module.setting.GroupSettingService;
import com.zincoid.nullbot.module.setting.SettingException;
import com.zincoid.nullbot.platform.persistence.enums.Access;
import com.zincoid.nullbot.platform.persistence.enums.Effort;
import com.zincoid.nullbot.platform.persistence.enums.Language;
import com.zincoid.nullbot.platform.persistence.enums.LimitScope;
import com.zincoid.nullbot.platform.persistence.po.GroupSetting;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * 群设置指令：查看或修改当前群的限速与 AI 设置。
 * <p>Group setting command: views or updates the current group's rate-limit and AI settings.</p>
 */
@CmdMapping({"GroupSet", "GSet", "群设置"})
@Component
@RequiredArgsConstructor
public class GroupSetCmd implements Cmd {

    private final GroupSettingService groupSettingService;
    private final GroupCmdRateLimiter groupCmdRateLimiter;
    private final LanguageService languageService;

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
        if (args.hasOpt("lang", "g")) {
            updateLanguage(ctx, groupId, args);
            return;
        }
        if (args.isEmpty() || "view".equalsIgnoreCase(args.peek())) {
            ctx.send(view(ctx, groupId));
            return;
        }
        throw new CmdWarnException(ctx.render(Text.GROUPSET_UNKNOWN_OPTION));
    }

    // ════ Update ══════════════════

    private void updateLimit(CmdContext ctx, String groupId, CmdArgs args) {
        String name = args.next().toLowerCase(Locale.ROOT);
        GroupSetting update = GroupSetting.builder().groupId(groupId).build();
        String message;
        switch (name) {
            case "enabled" -> {
                boolean enabled = bool(ctx, args.next());
                update.setLimitEnabled(enabled);
                message = ctx.render(Text.GROUPSET_LIMIT_ENABLED, enabled);
            }
            case "scope" -> {
                LimitScope scope = scope(ctx, args.next());
                update.setLimitScope(scope);
                message = ctx.render(Text.GROUPSET_LIMIT_SCOPE, scope.getLabel());
            }
            case "capacity" -> {
                int value = positive(ctx, args.nextInt());
                update.setLimitCapacity(value);
                message = ctx.render(Text.GROUPSET_LIMIT_CAPACITY, value);
            }
            case "refill" -> {
                int value = positive(ctx, args.nextInt());
                update.setLimitRefill(value);
                message = ctx.render(Text.GROUPSET_LIMIT_REFILL, value);
            }
            case "interval" -> {
                int value = positive(ctx, args.nextInt());
                update.setLimitInterval(value);
                message = ctx.render(Text.GROUPSET_LIMIT_INTERVAL, value);
            }
            default -> throw new CmdWarnException(ctx.render(Text.GROUPSET_UNKNOWN_LIMIT, name));
        }
        save(update);
        groupCmdRateLimiter.reset(groupId);
        ctx.send(Text.GROUPSET_UPDATED, message);
    }

    private void updateAi(CmdContext ctx, String groupId, CmdArgs args) {
        String name = args.next().toLowerCase(Locale.ROOT);
        GroupSetting update = GroupSetting.builder().groupId(groupId).build();
        String message;
        switch (name) {
            case "effort" -> {
                Effort effort = effort(ctx, args.next());
                update.setAiEffort(effort);
                message = ctx.render(Text.GROUPSET_AI_EFFORT, effort.getLabel());
            }
            case "prompt" -> {
                String text = args.rest("").trim();
                if (text.isEmpty()) throw new CmdWarnException(ctx.render(Text.GROUPSET_MISSING_PROMPT));
                boolean reset = "reset".equalsIgnoreCase(text);
                update.setAiPrompt(reset ? "" : text);
                message = ctx.render(reset ? Text.GROUPSET_AI_PROMPT_RESET : Text.GROUPSET_AI_PROMPT_UPDATED);
            }
            case "guard" -> {
                boolean enabled = bool(ctx, args.next());
                update.setAiInjectionGuard(enabled);
                message = ctx.render(Text.GROUPSET_AI_GUARD, enabled);
            }
            default -> throw new CmdWarnException(ctx.render(Text.GROUPSET_UNKNOWN_AI, name));
        }
        save(update);
        ctx.send(Text.GROUPSET_UPDATED, message);
    }

    private void updateLanguage(CmdContext ctx, String groupId, CmdArgs args) {
        String raw = args.getOpt("lang", "g", "");
        if (raw.isEmpty() || "true".equals(raw)) raw = args.next();
        Language language = Language.fromLabel(raw);
        if (language == null)
            throw new CmdWarnException(ctx.render(Text.GROUPSET_UNKNOWN_LANGUAGE, raw));
        save(GroupSetting.builder().groupId(groupId).language(language).build());
        ctx.send(Text.GROUPSET_UPDATED, ctx.render(Text.GROUPSET_LANGUAGE, language.getLabel()));
    }

    // ════ View ════════════════════

    private String view(CmdContext ctx, String groupId) {
        GroupSetting setting = groupSettingService.get(groupId);
        LimitScope scope = setting.getLimitScope() == null ? LimitScope.USER : setting.getLimitScope();
        Effort effort = setting.getAiEffort() == null ? Effort.HIGH : setting.getAiEffort();
        Language language = setting.getLanguage() == null ? Language.AUTO : setting.getLanguage();
        String languageText = language == Language.AUTO
                ? ctx.render(Text.GROUPSET_LANG_AUTO, languageService.defaultLanguage().getLabel())
                : language.getLabel();
        String prompt = setting.getAiPrompt() == null || setting.getAiPrompt().isBlank()
                ? ctx.render(Text.GROUPSET_DEFAULT)
                : setting.getAiPrompt();
        return ctx.render(Text.GROUPSET_VIEW,
                groupId, languageText,
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

    private static boolean bool(CmdContext ctx, String raw) {
        return switch (raw.toLowerCase(Locale.ROOT)) {
            case "on", "true", "1" -> true;
            case "off", "false", "0" -> false;
            default -> throw new CmdWarnException(ctx.render(Text.GROUPSET_MALFORMED_BOOLEAN, raw));
        };
    }

    private static int positive(CmdContext ctx, int value) {
        if (value <= 0) throw new CmdWarnException(ctx.render(Text.GROUPSET_VALUE_POSITIVE, value));
        return value;
    }

    private static LimitScope scope(CmdContext ctx, String raw) {
        for (LimitScope scope : LimitScope.values())
            if (scope.getLabel().equalsIgnoreCase(raw)) return scope;
        throw new CmdWarnException(ctx.render(Text.GROUPSET_UNKNOWN_SCOPE, raw));
    }

    private static Effort effort(CmdContext ctx, String raw) {
        for (Effort effort : Effort.values())
            if (effort.getLabel().equalsIgnoreCase(raw)) return effort;
        throw new CmdWarnException(ctx.render(Text.GROUPSET_UNKNOWN_EFFORT, raw));
    }

    // ════ Cmd ═════════════════════

    @Override
    public Access getAccess() {
        return Access.MANAGER;
    }

    @Override
    public String getHelp(Language language) {
        return language == Language.EN ? """
                GroupSet
                Views or updates the current group's settings.
                Usage: GroupSet [-v|--view]
                       GroupSet -l|--limit <enabled|scope|capacity|refill|interval> [value]
                       GroupSet -a|--ai <effort|prompt|guard> [value]
                       GroupSet -g|--lang <auto|zh|en>
                Values: scope=user|group|cmd, effort=minimal|low|medium|high|xhigh|max|ultra
                Note: "GroupSet ai prompt reset" clears the group prompt; "auto" follows the global default_language.
                Aliases: GSet / 群设置
                Access: >=1(manager)""" : """
                GroupSet
                查看或修改当前群的设置。
                用法：GroupSet [-v|--view]
                      GroupSet -l|--limit <enabled|scope|capacity|refill|interval> [value]
                      GroupSet -a|--ai <effort|prompt|guard> [value]
                      GroupSet -g|--lang <auto|zh|en>
                取值：scope=user|group|cmd, effort=minimal|low|medium|high|xhigh|max|ultra
                说明："GroupSet ai prompt reset" 清空群提示词；"auto" 跟随全局 default_language。
                别名：GSet / 群设置
                权限：>=1(manager)""";
    }
}
