package com.zincoid.symmetry;

import com.zincoid.nullbot.bot.cmd.Cmd;
import com.zincoid.nullbot.bot.plugin.Plugin;
import com.zincoid.nullbot.bot.plugin.PluginContext;
import com.zincoid.symmetry.cmd.SymmetryCmd;
import com.zincoid.symmetry.service.SymmetryHttp;

import java.util.List;

/**
 * 对称插件：注册图片对称指令。
 * <p>Symmetry plugin registering the image symmetry command.</p>
 */
public class SymmetryPlugin implements Plugin {

    private volatile SymmetryHttp http;

    @Override
    public String id() {
        return "symmetry";
    }

    @Override
    public int apiVersion() {
        return 5;
    }

    @Override
    public List<Cmd> cmds() {
        return List.of(new SymmetryCmd(http));
    }

    @Override
    public void start(PluginContext context) {
        this.http = new SymmetryHttp(context.http());
    }

    @Override
    public void stop() {
        this.http = null;
    }
}
