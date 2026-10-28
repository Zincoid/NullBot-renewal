package com.zincoid.sb6657;

import com.zincoid.nullbot.bot.cmd.Cmd;
import com.zincoid.nullbot.bot.plugin.Plugin;
import com.zincoid.nullbot.bot.plugin.PluginContext;
import com.zincoid.sb6657.cmd.SB6657Cmd;
import com.zincoid.sb6657.service.SB6657Http;

import java.util.List;

/**
 * SB6657 插件：注册随机玩机器烂梗指令。
 * <p>SB6657 plugin registering the random 玩机器 meme command.</p>
 */
public class SB6657Plugin implements Plugin {

    private volatile SB6657Http http;

    @Override
    public String id() {
        return "sb6657";
    }

    @Override
    public int apiVersion() {
        return 5;
    }

    @Override
    public List<Cmd> cmds() {
        return List.of(new SB6657Cmd(http));
    }

    @Override
    public void start(PluginContext context) {
        this.http = new SB6657Http(context.http());
    }

    @Override
    public void stop() {
        this.http = null;
    }
}
