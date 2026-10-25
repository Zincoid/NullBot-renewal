package com.zincoid.nullbot.bot.cmd.impl.system;

import com.zincoid.nullbot.bot.cmd.Cmd;
import com.zincoid.nullbot.bot.cmd.CmdArgs;
import com.zincoid.nullbot.bot.cmd.CmdContext;
import com.zincoid.nullbot.bot.cmd.CmdMapping;
import com.zincoid.nullbot.bot.cmd.CmdRegistry;
import com.zincoid.nullbot.bot.cmd.CmdWarnException;
import com.zincoid.nullbot.bot.plugin.loader.PluginLoader;
import com.zincoid.nullbot.bot.plugin.loader.PluginStatus;
import com.zincoid.nullbot.bot.spi.event.GroupMsgEvent;
import com.zincoid.nullbot.bot.spi.event.PrivateMsgEvent;
import com.zincoid.nullbot.bot.spi.model.MsgContent;
import com.zincoid.nullbot.module.i18n.Text;
import com.zincoid.nullbot.module.render.RenderService;
import com.zincoid.nullbot.platform.persistence.enums.Access;
import com.zincoid.nullbot.platform.persistence.enums.Language;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 帮助指令：列出内置指令、已加载插件，或指定插件的指令。
 * <p>Help command: lists built-in commands, loaded plugins, or the commands of one plugin.</p>
 */
@Slf4j
@CmdMapping({"Help", "帮助"})
@Component
@RequiredArgsConstructor
public class HelpCmd implements Cmd {

    private final ObjectProvider<CmdRegistry> registryProvider;
    private final ObjectProvider<PluginLoader> pluginLoaderProvider;
    private final RenderService renderService;

    // ════ Run ═════════════════════

    @Override
    public void run(CmdContext ctx, GroupMsgEvent event, CmdArgs args) {
        handle(ctx, args);
    }

    @Override
    public void run(CmdContext ctx, PrivateMsgEvent event, CmdArgs args) {
        handle(ctx, args);
    }

    private void handle(CmdContext ctx, CmdArgs args) {
        CmdRegistry registry = registryProvider.getObject();
        List<PluginStatus> plugins = pluginLoaderProvider.getObject().loaded();
        if (args.hasOpt("plugin", "p")) {
            String opt = args.getOpt("plugin", "p", "");
            String name = "true".equals(opt) ? args.next("") : opt;
            if (name.isBlank())
                throw new CmdWarnException(ctx.render(Text.HELP_PLUGIN_MISSING));
            PluginStatus plugin = plugins.stream()
                    .filter(item -> item.id().equals(name))
                    .findFirst()
                    .orElseThrow(() -> new CmdWarnException(
                            ctx.render(Text.HELP_PLUGIN_NOT_FOUND, name)));
            List<String> names = plugin.cmds().stream()
                    .map(registry::get)
                    .filter(Objects::nonNull)
                    .map(Cmd::name)
                    .distinct()
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .toList();
            sendImage(ctx, "PLUGIN", plugin.id(), names, "", "", "");
            log.info("Help sent: plugin={}, {} commands", plugin.id(), names.size());
            return;
        }
        Set<String> pluginNames = plugins.stream()
                .flatMap(plugin -> plugin.cmds().stream())
                .collect(Collectors.toSet());
        List<String> builtins = registry.getNames().stream()
                .filter(name -> !pluginNames.contains(name))
                .toList();
        String pluginIds = plugins.isEmpty()
                ? Text.HELP_NONE.of(ctx.language())
                : plugins.stream().map(PluginStatus::id).collect(Collectors.joining(", "));
        String hint = ctx.language() == Language.EN
                ? "View plugin commands: Help -p=name"
                : "查看插件指令：Help -p=插件名";
        sendImage(ctx, "HELP", "BUILT-IN COMMANDS", builtins, "PLUGINS", pluginIds, hint);
        log.info("Help sent: {} built-in commands, {} plugins", builtins.size(), plugins.size());
    }

    private void sendImage(CmdContext ctx, String heading, String title,
                           List<String> names, String footerLabel, String footer, String hint) {
        int rows = Math.max(1, (names.size() + 3) / 4);
        int height = 170 + rows * 26 + (footer.isBlank() ? 20 : 70);
        byte[] png;
        try {
            png = renderService.template("help.jte", Map.of(
                    "heading", heading,
                    "title", title,
                    "count", names.size() + (names.size() == 1 ? " COMMAND" : " COMMANDS"),
                    "names", names,
                    "rows", rows,
                    "height", height,
                    "footerLabel", footerLabel,
                    "footer", footer,
                    "hint", hint
            ));
        } catch (RuntimeException e) {
            log.warn("Help image render failed: title={}", title, e);
            throw new CmdWarnException(ctx.render(Text.HELP_RENDER_FAILED), e);
        }
        ctx.send(MsgContent.builder().imageBase64(png).build());
    }

    // ════ Cmd ═════════════════════

    @Override
    public Access getAccess() {
        return Access.RESTRICTED;
    }

    @Override
    public String getHelp(Language language) {
        return language == Language.EN ? """
                Help
                Lists built-in commands and loaded plugins, or the commands of one loaded plugin.
                Usage: Help [-p=<plugin> | --plugin=<plugin>]
                Aliases: 帮助
                Access: >=-1(restricted)""" : """
                Help
                列出内置指令与已加载插件；指定插件时列出该插件的指令。
                用法：Help [-p=插件名 | --plugin=插件名]
                别名：帮助
                权限：>=-1(restricted)""";
    }

    @Override
    public String getAiHelp() {
        return """
                Help
                Lists built-in commands and loaded plugins, or the commands of one loaded plugin.
                Usage: Help [-p=<plugin> | --plugin=<plugin>]""";
    }

    @Override
    public boolean isAiCallable() {
        return true;
    }
}
