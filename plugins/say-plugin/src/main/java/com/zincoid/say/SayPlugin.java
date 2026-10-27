package com.zincoid.say;

import com.zincoid.nullbot.bot.cmd.Cmd;
import com.zincoid.nullbot.bot.plugin.Plugin;
import com.zincoid.nullbot.bot.plugin.PluginContext;
import com.zincoid.say.cmd.SayCmd;

import java.util.List;

/**
 * 语录插件：注册 Say 指令，渲染随机语录卡片。
 * <p>Say plugin: registers the Say command that renders random quote cards.</p>
 */
public class SayPlugin implements Plugin {

    private volatile PluginContext context;

    @Override
    public String id() {
        return "say";
    }

    @Override
    public String version() {
        return "1.0";
    }

    @Override
    public int apiVersion() {
        return 3;
    }

    @Override
    public List<Cmd> cmds() {
        return List.of(new SayCmd(this));
    }

    @Override
    public void start(PluginContext context) {
        this.context = context;
    }

    @Override
    public void stop() {
        this.context = null;
    }

    /**
     * 返回插件上下文。
     * <p>Returns the plugin context.</p>
     *
     * @return the plugin context
     */
    public PluginContext context() {
        PluginContext ctx = context;
        if (ctx == null) throw new IllegalStateException("Plugin not started");
        return ctx;
    }
}
