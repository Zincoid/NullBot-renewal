package com.zincoid.nullbot.bot.cmd.impl.chat;

import com.zincoid.nullbot.bot.chat.ChatFlow;
import com.zincoid.nullbot.bot.cmd.Cmd;
import com.zincoid.nullbot.bot.cmd.CmdArgs;
import com.zincoid.nullbot.bot.cmd.CmdContext;
import com.zincoid.nullbot.bot.cmd.CmdMapping;
import com.zincoid.nullbot.bot.spi.event.GroupMsgEvent;
import com.zincoid.nullbot.bot.spi.event.PrivateMsgEvent;
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

    private final ObjectProvider<ChatFlow> chatFlow;

    // ════ Run ═════════════════════

    @Override
    public void run(CmdContext ctx, GroupMsgEvent event, CmdArgs args) {
        chatFlow.getObject().chat(ctx.client(), event);
    }

    @Override
    public void run(CmdContext ctx, PrivateMsgEvent event, CmdArgs args) {
        chatFlow.getObject().chat(ctx.client(), event);
    }

    // ════ Cmd ═════════════════════

    @Override
    public String getHelp() {
        return """
                Chat
                Talks with the AI.
                Usage: Chat [content]
                Aliases: 对话
                Access: >=0(normal)""";
    }
}
