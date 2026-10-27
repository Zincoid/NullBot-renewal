package com.zincoid.sb6657;

import com.zincoid.nullbot.bot.cmd.Cmd;
import com.zincoid.nullbot.bot.plugin.Plugin;
import com.zincoid.sb6657.cmd.SB6657Cmd;

import java.util.List;

/**
 * SB6657 插件：注册随机玩机器烂梗指令。
 * <p>SB6657 plugin registering the random 玩机器 meme command.</p>
 */
public class SB6657Plugin implements Plugin {

    @Override
    public String id() {
        return "sb6657";
    }

    @Override
    public String version() {
        return "1.0";
    }

    @Override
    public List<Cmd> cmds() {
        return List.of(new SB6657Cmd());
    }
}
