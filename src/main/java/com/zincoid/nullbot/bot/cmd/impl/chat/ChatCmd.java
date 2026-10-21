package com.zincoid.nullbot.bot.cmd.impl.chat;

import com.zincoid.nullbot.bot.chat.ChatFlow;
import com.zincoid.nullbot.bot.cmd.Cmd;
import com.zincoid.nullbot.bot.cmd.CmdArgs;
import com.zincoid.nullbot.bot.cmd.CmdContext;
import com.zincoid.nullbot.bot.cmd.CmdMapping;
import com.zincoid.nullbot.bot.spi.event.GroupMsgEvent;
import com.zincoid.nullbot.bot.spi.event.PrivateMsgEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * AI 对话指令。
 * <p>AI chat command.</p>
 */
@CmdMapping({"Chat", "对话"})
@Component
@RequiredArgsConstructor
public class ChatCmd implements Cmd {

    private final ChatFlow chatFlow;

    @Override
    public void run(CmdContext ctx, GroupMsgEvent event, CmdArgs args) {
        chatFlow.chatGroup(
                ctx.client(),
                event.groupId(),
                event.selfId(),
                event.messageId()
        );
    }

    @Override
    public void run(CmdContext ctx, PrivateMsgEvent event, CmdArgs args) {
        chatFlow.chatPrivate(
                ctx.client(),
                event.userId(),
                event.selfId(),
                event.messageId()
        );
    }

    @Override
    public String getHelp() {
        return """
                Chat
                Talks with the AI.
                Usage: Chat [content]
                Aliases: 对话
                Access: >=NORMAL""";
    }
}
