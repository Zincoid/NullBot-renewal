package com.zincoid.nullbot.bot.cmd.impl;

import com.zincoid.nullbot.bot.cmd.Cmd;
import com.zincoid.nullbot.bot.cmd.CmdArgs;
import com.zincoid.nullbot.bot.cmd.CmdContext;
import com.zincoid.nullbot.bot.cmd.CmdMapping;
import com.zincoid.nullbot.bot.spi.event.GroupMsgEvent;
import com.zincoid.nullbot.bot.spi.event.PrivateMsgEvent;
import com.zincoid.nullbot.platform.persistence.enums.Language;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Echo 指令：把参数文本原样发送到当前会话。
 * <p>Echo command: sends the argument text back to the current chat.</p>
 */
@Slf4j
@CmdMapping({"Echo", "回响"})
@Component
public class EchoCmd implements Cmd {

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
        log.info("Echo sent: {}", message);
    }

    // ════ Cmd ═════════════════════

    @Override
    public String getHelp(Language language) {
        return language == Language.EN ? """
                Echo
                Sends the given text to the current chat.
                Usage: Echo <text>
                Aliases: 回响
                Access: >=0(normal)""" : """
                Echo
                把给定文本发送到当前会话。
                用法：Echo <文本>
                别名: 回响
                权限：>=0(normal)""";
    }
}
