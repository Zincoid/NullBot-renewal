package com.zincoid.nullbot.bot.cmd.impl.manage;

import com.zincoid.nullbot.bot.cmd.Cmd;
import com.zincoid.nullbot.bot.cmd.CmdArgs;
import com.zincoid.nullbot.bot.cmd.CmdContext;
import com.zincoid.nullbot.bot.cmd.CmdMapping;
import com.zincoid.nullbot.bot.exception.CmdWarnException;
import com.zincoid.nullbot.bot.spi.event.GroupMsgEvent;
import com.zincoid.nullbot.bot.spi.event.PrivateMsgEvent;
import com.zincoid.nullbot.module.config.Conf;
import com.zincoid.nullbot.module.config.ConfigException;
import com.zincoid.nullbot.module.config.ConfigService;
import com.zincoid.nullbot.platform.persistence.enums.Access;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

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
            ctx.send(list());
            return;
        }
        Conf conf = conf(args.next());
        if (!args.hasNext()) {
            ctx.send(conf.getKey() + " = " + display(conf, configService.get(conf)));
            return;
        }
        String value = args.rest();
        if ("reset".equalsIgnoreCase(value)) {
            reset(conf);
            ctx.send("✅ Config reset: " + conf.getKey());
            return;
        }
        save(conf, value);
        ctx.send("✅ Config updated: " + conf.getKey() + " = " + display(conf, value));
    }

    // ════ Helpers ═════════════════

    private String list() {
        StringBuilder text = new StringBuilder("Config:");
        for (Conf conf : Conf.values())
            text.append("\n").append(conf.getKey()).append(" = ")
                    .append(display(conf, configService.get(conf)));
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

    private static Conf conf(String raw) {
        for (Conf conf : Conf.values())
            if (conf.getKey().equalsIgnoreCase(raw) || conf.name().equalsIgnoreCase(raw))
                return conf;
        throw new CmdWarnException("Unknown config key: " + raw);
    }

    private static String display(Conf conf, String value) {
        if (conf == Conf.AI_PROVIDER_API_KEY && value != null && !value.isBlank()) return "****";
        return value == null || value.isEmpty() ? "(empty)" : value;
    }

    // ════ Cmd ═════════════════════

    @Override
    public Access getAccess() {
        return Access.ADMIN;
    }

    @Override
    public String getHelp() {
        return """
                Config
                Views or updates bot configuration.
                Usage: Config
                       Config <key>
                       Config <key> <value>
                       Config <key> reset
                Keys: cmd_prefix, message_history_max_length, ai_provider_api_url,
                      ai_provider_api_key, ai_provider_model, ai_max_tokens,
                      ai_max_tool_calls, ai_chat_prompt
                Aliases: 配置
                Access: >=ADMIN""";
    }
}
