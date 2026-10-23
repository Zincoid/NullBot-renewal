package com.zincoid.nullbot.bot.cmd.impl;

import com.zincoid.nullbot.bot.cmd.Cmd;
import com.zincoid.nullbot.bot.cmd.CmdArgs;
import com.zincoid.nullbot.bot.cmd.CmdContext;
import com.zincoid.nullbot.bot.cmd.CmdMapping;
import com.zincoid.nullbot.bot.cmd.CmdRegistry;
import com.zincoid.nullbot.bot.spi.event.GroupMsgEvent;
import com.zincoid.nullbot.bot.spi.event.PrivateMsgEvent;
import com.zincoid.nullbot.module.i18n.Text;
import com.zincoid.nullbot.platform.persistence.enums.Access;
import com.zincoid.nullbot.platform.persistence.enums.Language;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 帮助指令：列出当前已注册的指令。
 * <p>Help command: lists the registered commands.</p>
 */
@Slf4j
@CmdMapping({"Help", "帮助"})
@Component
@RequiredArgsConstructor
public class HelpCmd implements Cmd {

    private final ObjectProvider<CmdRegistry> registryProvider;

    // ════ Run ═════════════════════

    @Override
    public void run(CmdContext ctx, GroupMsgEvent event, CmdArgs args) {
        handle(ctx);
    }

    @Override
    public void run(CmdContext ctx, PrivateMsgEvent event, CmdArgs args) {
        handle(ctx);
    }

    private void handle(CmdContext ctx) {
        List<String> names = registryProvider.getObject().getNames();
        ctx.send(Text.HELP_LIST, names.size(), String.join(", ", names));
        log.info("Help sent: {} commands", names.size());
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
                Lists all registered commands.
                Usage: Help
                Aliases: 帮助
                Access: >=-1(restricted)""" : """
                Help
                列出当前已注册的指令。
                用法：Help
                别名：帮助
                权限：>=-1(restricted)""";
    }
}
