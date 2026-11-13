package com.zincoid.nullbot.bot.aichat;

import com.zincoid.nullbot.bot.BotClients;
import com.zincoid.nullbot.bot.cmd.CmdDispatcher;
import com.zincoid.nullbot.bot.cmd.CmdRequest;
import com.zincoid.nullbot.bot.spi.event.BotEvent;
import com.zincoid.nullbot.bot.spi.event.GroupMsgEvent;
import com.zincoid.nullbot.bot.spi.event.GroupPokeEvent;
import com.zincoid.nullbot.bot.spi.event.MsgEvent;
import com.zincoid.nullbot.bot.spi.event.PokeEvent;
import com.zincoid.nullbot.bot.spi.event.PrivateMsgEvent;
import com.zincoid.nullbot.bot.spi.event.PrivatePokeEvent;
import com.zincoid.nullbot.bot.spi.model.MsgContent;
import com.zincoid.nullbot.module.ai.tool.ToolRes;
import com.zincoid.nullbot.module.config.Conf;
import com.zincoid.nullbot.module.config.ConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * 聊天指令：在当前会话中执行机器人指令，并把 poke 事件转换为可分发的消息事件。
 * <p>Chat commands: runs bot commands in the current chat and converts poke events into dispatchable message events.</p>
 */
@Component
@RequiredArgsConstructor
public class ChatCommands {

    private final CmdDispatcher cmdDispatcher;
    private final ConfigService configService;

    // ════ Command ═════════════════

    /**
     * 执行机器人指令。
     * <p>Runs a bot command.</p>
     *
     * @param turn   the turn data
     * @param name   command name
     * @param params command parameters
     * @return the tool result
     */
    ToolRes cmd(ChatTurn turn, String name, List<String> params) {
        MsgEvent cmdEvent = cmdEvent(turn);
        if (cmdEvent == null)
            return ToolRes.error("Command not available for this event");
        boolean checkAccess = configService.getBool(Conf.AI_CMD_ACCESS_CHECK);
        cmdDispatcher.dispatch(turn.client(), cmdEvent, new CmdRequest(name, params), checkAccess);
        return ToolRes.of("Command executed: " + name);
    }

    private static MsgEvent cmdEvent(ChatTurn turn) {
        BotEvent event = turn.event();
        if (event instanceof MsgEvent e) return e;
        if (!(event instanceof PokeEvent)) return null;
        String messageId = UUID.randomUUID().toString();
        String nickname = BotClients.userName(turn.client(), event.userId());
        MsgContent content = MsgContent.of("");
        if (event instanceof GroupPokeEvent e)
            return new GroupMsgEvent(e.selfId(), e.userId(), e.groupId(),
                    messageId, nickname, content, false, false);
        if (event instanceof PrivatePokeEvent e)
            return new PrivateMsgEvent(e.selfId(), e.userId(),
                    messageId, nickname, content, false, false);
        return null;
    }
}
