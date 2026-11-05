package com.zincoid.nullbot.bot.cmd.builtin.admin;

import com.zincoid.nullbot.bot.cmd.Cmd;
import com.zincoid.nullbot.bot.cmd.CmdArgs;
import com.zincoid.nullbot.bot.cmd.CmdContext;
import com.zincoid.nullbot.bot.cmd.CmdMapping;
import com.zincoid.nullbot.bot.cmd.CmdWarnException;
import com.zincoid.nullbot.bot.spi.event.GroupMsgEvent;
import com.zincoid.nullbot.module.i18n.LanguageService;
import com.zincoid.nullbot.module.i18n.Text;
import com.zincoid.nullbot.module.setting.GroupRateLimiter;
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
 * 设置指令：查看或修改当前会话的通知、限速与 AI 设置。
 * <p>Setting command: views or updates the current chat's notice, rate-limit, and AI settings.</p>
 */
@CmdMapping({"Setting", "设置"})
@Component
@RequiredArgsConstructor
public class SettingCmd implements Cmd {

    private final GroupSettingService groupSettingService;
    private final GroupRateLimiter groupRateLimiter;
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
        if (args.hasOpt("notice", "n")) {
            updateNotice(ctx, groupId, args);
            return;
        }
        if (args.hasOpt("anti-recall", "r")) {
            updateAntiRecall(ctx, groupId, args);
            return;
        }
        if (args.isEmpty() || "view".equalsIgnoreCase(args.peek())) {
            ctx.send(view(ctx, groupId));
            return;
        }
        throw ctx.warn(Text.SETTING_UNKNOWN_OPTION);
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
                message = ctx.render(Text.SETTING_LIMIT_ENABLED, enabled);
            }
            case "scope" -> {
                LimitScope scope = scope(ctx, args.next());
                update.setLimitScope(scope);
                message = ctx.render(Text.SETTING_LIMIT_SCOPE, scope.getLabel());
            }
            case "capacity" -> {
                int value = positive(ctx, args.nextInt());
                update.setLimitCapacity(value);
                message = ctx.render(Text.SETTING_LIMIT_CAPACITY, value);
            }
            case "refill" -> {
                int value = positive(ctx, args.nextInt());
                update.setLimitRefill(value);
                message = ctx.render(Text.SETTING_LIMIT_REFILL, value);
            }
            case "interval" -> {
                int value = positive(ctx, args.nextInt());
                update.setLimitInterval(value);
                message = ctx.render(Text.SETTING_LIMIT_INTERVAL, value);
            }
            default -> throw ctx.warn(Text.SETTING_UNKNOWN_LIMIT, name);
        }
        save(update);
        groupRateLimiter.reset(groupId);
        ctx.send(Text.SETTING_UPDATED, message);
    }

    private void updateAi(CmdContext ctx, String groupId, CmdArgs args) {
        String name = args.next().toLowerCase(Locale.ROOT);
        GroupSetting update = GroupSetting.builder().groupId(groupId).build();
        String message;
        switch (name) {
            case "effort" -> {
                Effort effort = effort(ctx, args.next());
                update.setAiEffort(effort);
                message = ctx.render(Text.SETTING_AI_EFFORT, effort.getLabel());
            }
            case "prompt" -> {
                String text = args.rest("").trim();
                if (text.isEmpty()) throw ctx.warn(Text.SETTING_AI_PROMPT_MISSING);
                boolean reset = "reset".equalsIgnoreCase(text);
                update.setAiPrompt(reset ? "" : text);
                message = ctx.render(reset ? Text.SETTING_AI_PROMPT_RESET : Text.SETTING_AI_PROMPT_UPDATED);
            }
            case "guard" -> {
                boolean enabled = bool(ctx, args.next());
                update.setAiInjectionGuard(enabled);
                message = ctx.render(Text.SETTING_AI_GUARD, enabled);
            }
            case "auto" -> {
                boolean enabled = bool(ctx, args.next());
                update.setAiAutoChatEnabled(enabled);
                message = ctx.render(Text.SETTING_AI_AUTO_CHAT, enabled);
            }
            case "frequency" -> {
                double value = frequency(ctx, args.nextDouble());
                update.setAiChatFrequency(value);
                message = ctx.render(Text.SETTING_AI_CHAT_FREQUENCY, value);
            }
            default -> throw ctx.warn(Text.SETTING_UNKNOWN_AI, name);
        }
        save(update);
        ctx.send(Text.SETTING_UPDATED, message);
    }

    private void updateLanguage(CmdContext ctx, String groupId, CmdArgs args) {
        String raw = args.getOpt("lang", "g", "");
        if (raw.isEmpty() || "true".equals(raw)) raw = args.next();
        Language language = Language.fromLabel(raw);
        if (language == null)
            throw ctx.warn(Text.SETTING_UNKNOWN_LANGUAGE, raw);
        save(GroupSetting.builder().groupId(groupId).language(language).build());
        ctx.send(Text.SETTING_UPDATED, ctx.render(Text.SETTING_LANGUAGE, language.getLabel()));
    }

    private void updateNotice(CmdContext ctx, String groupId, CmdArgs args) {
        String name = args.next().toLowerCase(Locale.ROOT);
        boolean enabled = bool(ctx, args.next());
        GroupSetting update = GroupSetting.builder().groupId(groupId).build();
        Text message;
        switch (name) {
            case "join" -> {
                update.setJoinNoticeEnabled(enabled);
                message = Text.SETTING_NOTICE_JOIN;
            }
            case "leave" -> {
                update.setLeaveNoticeEnabled(enabled);
                message = Text.SETTING_NOTICE_LEAVE;
            }
            default -> throw ctx.warn(Text.SETTING_UNKNOWN_NOTICE, name);
        }
        save(update);
        ctx.send(Text.SETTING_UPDATED, ctx.render(message, enabled));
    }

    private void updateAntiRecall(CmdContext ctx, String groupId, CmdArgs args) {
        boolean enabled = bool(ctx, args.next());
        save(GroupSetting.builder().groupId(groupId).antiRecallEnabled(enabled).build());
        ctx.send(Text.SETTING_UPDATED, ctx.render(Text.SETTING_ANTI_RECALL, enabled));
    }

    // ════ View ════════════════════

    private String view(CmdContext ctx, String groupId) {
        GroupSetting setting = groupSettingService.get(groupId);
        Language language = setting.getLanguage();
        String languageText = language == Language.AUTO
                ? ctx.render(Text.SETTING_LANG_AUTO, languageService.defaultLanguage().getLabel())
                : language.getLabel();
        String prompt = setting.getAiPrompt() == null || setting.getAiPrompt().isBlank()
                ? ctx.render(Text.SETTING_DEFAULT)
                : setting.getAiPrompt();
        return ctx.render(Text.SETTING_VIEW,
                groupId, languageText,
                setting.getJoinNoticeEnabled(), setting.getLeaveNoticeEnabled(),
                setting.getAntiRecallEnabled(),
                setting.getLimitEnabled(), setting.getLimitScope().getLabel(),
                setting.getLimitCapacity(), setting.getLimitRefill(), setting.getLimitInterval(),
                setting.getAiEffort().getLabel(), setting.getAiInjectionGuard(),
                setting.getAiAutoChatEnabled(), setting.getAiChatFrequency(), prompt);
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
            default -> throw ctx.warn(Text.SETTING_MALFORMED_BOOLEAN, raw);
        };
    }

    private static int positive(CmdContext ctx, int value) {
        if (value <= 0) throw ctx.warn(Text.SETTING_POSITIVE_VALUE, value);
        return value;
    }

    private static double frequency(CmdContext ctx, double value) {
        if (!Double.isFinite(value) || value < 0 || value > 1)
            throw ctx.warn(Text.SETTING_FREQUENCY_RANGE, value);
        return value;
    }

    private static LimitScope scope(CmdContext ctx, String raw) {
        LimitScope scope = LimitScope.fromLabel(raw);
        if (scope == null)
            throw ctx.warn(Text.SETTING_UNKNOWN_SCOPE, raw);
        return scope;
    }

    private static Effort effort(CmdContext ctx, String raw) {
        Effort effort = Effort.fromLabel(raw);
        if (effort == null)
            throw ctx.warn(Text.SETTING_UNKNOWN_EFFORT, raw);
        return effort;
    }

    // ════ Cmd ═════════════════════

    @Override
    public Access getAccess() {
        return Access.MANAGER;
    }

    @Override
    public String getHelp(Language language) {
        return language == Language.EN ? """
                Setting
                Views or updates the current chat's settings.
                Usage: Setting [-v|--view]
                       Setting -l|--limit <enabled|scope|capacity|refill|interval> [value]
                       Setting -a|--ai <effort|prompt|guard|auto|frequency> [value]
                       Setting -g|--lang <auto|zh|en>
                       Setting -n|--notice <join|leave> <on|off>
                       Setting -r|--anti-recall <on|off>
                Values: scope=user|group|func, effort=minimal|low|medium|high|xhigh|max|ultra, frequency=0..1
                Note: "Setting ai prompt reset" clears the current chat prompt; "auto" follows the global default_language.
                Aliases: 设置
                Access: >=1(manager)""" : """
                Setting
                查看或修改当前会话的设置。
                用法：Setting [-v|--view]
                      Setting -l|--limit <enabled|scope|capacity|refill|interval> [value]
                      Setting -a|--ai <effort|prompt|guard|auto|frequency> [value]
                      Setting -g|--lang <auto|zh|en>
                      Setting -n|--notice <join|leave> <on|off>
                      Setting -r|--anti-recall <on|off>
                取值：scope=user|group|func, effort=minimal|low|medium|high|xhigh|max|ultra, frequency=0~1
                说明："Setting ai prompt reset" 清空当前会话提示词；"auto" 跟随全局 default_language。
                别名：设置
                权限：>=1(manager)""";
    }
}
