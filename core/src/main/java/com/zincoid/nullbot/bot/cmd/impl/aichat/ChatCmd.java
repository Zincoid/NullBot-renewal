package com.zincoid.nullbot.bot.cmd.impl.aichat;

import com.zincoid.nullbot.bot.chat.ChatPipeline;
import com.zincoid.nullbot.bot.cmd.Cmd;
import com.zincoid.nullbot.bot.cmd.CmdArgs;
import com.zincoid.nullbot.bot.cmd.CmdContext;
import com.zincoid.nullbot.bot.cmd.CmdMapping;
import com.zincoid.nullbot.bot.spi.event.GroupMsgEvent;
import com.zincoid.nullbot.bot.spi.event.PrivateMsgEvent;
import com.zincoid.nullbot.platform.persistence.enums.Language;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * AI 对话指令。
 * <p>AI chat command.</p>
 */
@CmdMapping({"Chat", "对话"})
@Component
@RequiredArgsConstructor
public class ChatCmd implements Cmd {

    private final ObjectProvider<ChatPipeline> chatPipeline;

    // ════ Run ═════════════════════

    @Override
    public void run(CmdContext ctx, GroupMsgEvent event, CmdArgs args) {
        chatPipeline.getObject().chat(ctx.client(), event, ctx.language());
    }

    @Override
    public void run(CmdContext ctx, PrivateMsgEvent event, CmdArgs args) {
        chatPipeline.getObject().chat(ctx.client(), event, ctx.language());
    }

    // ════ Cmd ═════════════════════

    @Override
    public String getHelp(Language language) {
        return language == Language.EN ? """
                Chat
                Talks with the AI.
                Usage: Chat [content]
                Aliases: 对话
                Access: >=0(normal)""" : """
                Chat
                与 AI 对话。
                用法：Chat [content]
                别名：对话
                权限：>=0(normal)""";
    }
}
