package com.zincoid.nullbot.bot.cmd.impl.chat;

import com.zincoid.nullbot.bot.chat.ChatFlow;
import com.zincoid.nullbot.bot.cmd.Cmd;
import com.zincoid.nullbot.bot.cmd.CmdArgs;
import com.zincoid.nullbot.bot.cmd.CmdContext;
import com.zincoid.nullbot.bot.cmd.CmdMapping;
import com.zincoid.nullbot.bot.spi.event.GroupPokeEvent;
import com.zincoid.nullbot.bot.spi.event.PrivatePokeEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 戳一戳指令：被戳时触发一次 AI 对话。
 * <p>Poke command: triggers one AI conversation when poked.</p>
 */
@CmdMapping("Poked")
@Component
@RequiredArgsConstructor
public class PokedCmd implements Cmd {

    private final ChatFlow chatFlow;

    // ════ Run ═════════════════════

    @Override
    public void run(CmdContext ctx, GroupPokeEvent event, CmdArgs args) {
        chatFlow.pokeGroup(
                ctx.client(),
                event.groupId(),
                event.selfId(),
                event.userId()
        );
    }

    @Override
    public void run(CmdContext ctx, PrivatePokeEvent event, CmdArgs args) {
        chatFlow.pokePrivate(
                ctx.client(),
                event.userId(),
                event.selfId()
        );
    }

    // ════ Cmd ═════════════════════

    @Override
    public String getHelp() {
        return """
                Poked - used for poke event""";
    }
}
