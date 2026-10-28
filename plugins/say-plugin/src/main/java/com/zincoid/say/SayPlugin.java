package com.zincoid.say;

import com.zincoid.nullbot.bot.cmd.Cmd;
import com.zincoid.nullbot.bot.plugin.Plugin;
import com.zincoid.nullbot.bot.plugin.PluginContext;
import com.zincoid.say.cmd.SayCmd;
import com.zincoid.say.service.SayHttp;

import java.util.List;

/**
 * 语录插件：注册 Say 指令，渲染随机语录卡片。
 * <p>Say plugin: registers the Say command that renders random quote cards.</p>
 */
public class SayPlugin implements Plugin {

    private volatile PluginContext context;
    private volatile SayHttp http;

    @Override
    public String id() {
        return "say";
    }

    @Override
    public int apiVersion() {
        return 5;
    }

    @Override
    public List<Cmd> cmds() {
        return List.of(new SayCmd(context, http));
    }

    @Override
    public void start(PluginContext context) {
        this.context = context;
        this.http = new SayHttp(context.http());
    }

    @Override
    public void stop() {
        this.context = null;
        this.http = null;
    }
}
