package com.zincoid.nullbot.bot.cmd.impl.admin;

import com.zincoid.nullbot.bot.cmd.Cmd;
import com.zincoid.nullbot.bot.cmd.CmdArgs;
import com.zincoid.nullbot.bot.cmd.CmdContext;
import com.zincoid.nullbot.bot.cmd.CmdMapping;
import com.zincoid.nullbot.bot.cmd.CmdWarnException;
import com.zincoid.nullbot.bot.spi.event.GroupMsgEvent;
import com.zincoid.nullbot.bot.spi.event.PrivateMsgEvent;
import com.zincoid.nullbot.module.config.Conf;
import com.zincoid.nullbot.module.config.ConfigException;
import com.zincoid.nullbot.module.config.ConfigService;
import com.zincoid.nullbot.module.i18n.Text;
import com.zincoid.nullbot.platform.persistence.enums.Access;
import com.zincoid.nullbot.platform.persistence.enums.Language;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * 配置指令：查看或修改机器人配置。
 * <p>Config command: views or updates bot configuration.</p>
 */
@CmdMapping({"Config", "配置"})
@Component
@RequiredArgsConstructor
public class ConfigCmd implements Cmd {

    private final ConfigService configService;

    // ════ Run ═════════════════════

    @Override
    public void run(CmdContext ctx, GroupMsgEvent event, CmdArgs args) {
        handle(ctx, args);
    }

    @Override
    public void run(CmdContext ctx, PrivateMsgEvent event, CmdArgs args) {
        handle(ctx, args);
    }

    // ════ Handle ══════════════════

    private void handle(CmdContext ctx, CmdArgs args) {
        if (args.isEmpty()) {
            ctx.send(list(ctx));
            return;
        }
        Conf conf = conf(ctx, args.next());
        if (!args.hasNext()) {
            ctx.send(conf.getKey() + " = " + display(ctx, conf, configService.getString(conf)));
            return;
        }
        String value = args.rest();
        if ("reset".equalsIgnoreCase(value)) {
            reset(conf);
            ctx.send(Text.CONFIG_RESET, conf.getKey());
            return;
        }
        value = validate(ctx, conf, value);
        save(conf, value);
        ctx.send(Text.CONFIG_UPDATED, conf.getKey(), display(ctx, conf, value));
    }

    // ════ Helpers ═════════════════

    private String list(CmdContext ctx) {
        StringBuilder text = new StringBuilder(ctx.render(Text.CONFIG_HEADER));
        for (Conf conf : Conf.values())
            text.append("\n").append(conf.getKey()).append(" = ")
                    .append(display(ctx, conf, configService.getString(conf)));
        return text.toString();
    }

    private void save(Conf conf, String value) {
        try {
            configService.update(conf, value);
        } catch (ConfigException e) {
            throw new CmdWarnException(e.getMessage());
        }
    }

    private void reset(Conf conf) {
        save(conf, conf.getDefaultValue());
    }

    private static Conf conf(CmdContext ctx, String raw) {
        for (Conf conf : Conf.values())
            if (conf.getKey().equalsIgnoreCase(raw) || conf.name().equalsIgnoreCase(raw))
                return conf;
        throw new CmdWarnException(ctx.render(Text.CONFIG_UNKNOWN_KEY, raw));
    }

    private static String validate(CmdContext ctx, Conf conf, String value) {
        if (conf != Conf.DEFAULT_LANGUAGE) return value;
        Language language = Language.fromLabel(value);
        if (language != Language.ZH && language != Language.EN)
            throw new CmdWarnException(ctx.render(Text.CONFIG_INVALID_LANGUAGE, value));
        return language.getLabel();
    }

    private static String display(CmdContext ctx, Conf conf, String value) {
        if (conf == Conf.AI_PROVIDER_API_KEY && value != null && !value.isBlank()) return "****";
        return value == null || value.isEmpty() ? ctx.render(Text.CONFIG_EMPTY) : value;
    }

    // ════ Cmd ═════════════════════

    @Override
    public Access getAccess() {
        return Access.ADMIN;
    }

    @Override
    public String getHelp(Language language) {
        String keys = Arrays.stream(Conf.values())
                .map(Conf::getKey)
                .collect(Collectors.joining(", "));
        return language == Language.EN ? """
                Config
                Views or updates bot configuration.
                Usage: Config
                       Config <key>
                       Config <key> <value>
                       Config <key> reset
                Keys: %s
                Aliases: 配置
                Access: >=2(admin)""".formatted(keys) : """
                Config
                查看或修改机器人配置。
                用法：Config
                      Config <key>
                      Config <key> <value>
                      Config <key> reset
                配置项：%s
                别名：配置
                权限：>=2(admin)""".formatted(keys);
    }
}
