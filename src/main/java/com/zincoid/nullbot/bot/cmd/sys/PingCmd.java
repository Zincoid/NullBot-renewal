package com.zincoid.nullbot.bot.cmd.sys;

import com.zincoid.nullbot.bot.cmd.Cmd;
import com.zincoid.nullbot.bot.cmd.CmdArgs;
import com.zincoid.nullbot.bot.cmd.CmdContext;
import com.zincoid.nullbot.bot.cmd.CmdMapping;
import com.zincoid.nullbot.bot.spi.event.GroupMsgEvent;
import com.zincoid.nullbot.bot.spi.event.PrivateMsgEvent;
import org.springframework.stereotype.Component;

/**
 * Ping command for connectivity smoke tests.
 * <p>连通性冒烟测试用 Ping 指令。</p>
 */
@CmdMapping("Ping")
@Component
public class PingCmd implements Cmd {

    @Override
    public void run(CmdContext ctx, GroupMsgEvent event, CmdArgs args) {
        ctx.send("pong");
    }

    @Override
    public void run(CmdContext ctx, PrivateMsgEvent event, CmdArgs args) {
        ctx.send("pong");
    }

    @Override
    public String getHelp() {
        return "Ping - connectivity test";
    }
}
