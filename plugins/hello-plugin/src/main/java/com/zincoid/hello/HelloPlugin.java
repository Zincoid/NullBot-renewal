package com.zincoid.hello;

import com.zincoid.nullbot.bot.cmd.Cmd;
import com.zincoid.nullbot.bot.plugin.Plugin;
import com.zincoid.nullbot.bot.plugin.PluginContext;
import com.zincoid.nullbot.module.ai.tool.Tool;

import java.util.List;

/**
 * 示例插件：注册 Hello 指令与测试工具。
 * <p>Sample plugin registering the Hello command and a test tool.</p>
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
    public int apiVersion() {
        return 3;
    }

    @Override
    public List<Cmd> cmds() {
        return List.of(new HelloCmd(this));
    }

    @Override
    public List<Tool> tools() {
        return List.of(new HelloTool());
    }

    @Override
    public void start(PluginContext context) {
        this.context = context;
    }

    public PluginContext getContext() {
        return context;
    }
}
