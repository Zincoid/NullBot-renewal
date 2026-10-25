package com.zincoid.hello;

import com.zincoid.nullbot.bot.cmd.Cmd;
import com.zincoid.nullbot.bot.plugin.Plugin;
import com.zincoid.nullbot.bot.plugin.PluginContext;

import java.nio.file.Path;
import java.util.List;

/**
 * 示例插件：注册 Hello 指令。
 * <p>Sample plugin registering the Hello command.</p>
 */
public class HelloPlugin implements Plugin {

    private PluginContext context;

    @Override
    public String id() {
        return "hello";
    }

    @Override
    public String version() {
        return "1.0";
    }

    @Override
    public List<Cmd> cmds() {
        return List.of(new HelloCmd(this));
    }

    @Override
    public void start(PluginContext context) {
        this.context = context;
    }

    public PluginContext getContext() {
        return context;
    }
}
