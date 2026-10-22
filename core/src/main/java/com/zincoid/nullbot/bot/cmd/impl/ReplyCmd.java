package com.zincoid.nullbot.bot.cmd.impl;

import com.zincoid.nullbot.bot.cmd.Cmd;
import com.zincoid.nullbot.bot.cmd.CmdArgs;
import com.zincoid.nullbot.bot.cmd.CmdContext;
import com.zincoid.nullbot.bot.cmd.CmdMapping;
import com.zincoid.nullbot.bot.spi.event.GroupMsgEvent;
import com.zincoid.nullbot.bot.spi.event.PrivateMsgEvent;
import com.zincoid.nullbot.platform.persistence.enums.Access;
import com.zincoid.nullbot.platform.persistence.enums.Language;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 回复指令：把参数文本原样发送到当前会话。
 * <p>Reply command: sends the argument text back to the current chat.</p>
 */
@Slf4j
@CmdMapping({"Reply", "回复"})
@Component
public class ReplyCmd implements Cmd {

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
        String message = args.rest();
        ctx.send(message);
        log.info("Reply sent: {}", message);
    }

    // ════ Cmd ═════════════════════

    @Override
    public Access getAccess() {
        return Access.MANAGER;
    }

    @Override
    public String getHelp(Language language) {
        return language == Language.EN ? """
                Reply
                Sends the given text to the current chat.
                Usage: Reply <text>
                Aliases: 回复
                Access: >=1(manager)""" : """
                Reply
                把给定文本发送到当前会话。
                用法：Reply <文本>
                别名：回复
                权限：>=1(manager)""";
    }
}
