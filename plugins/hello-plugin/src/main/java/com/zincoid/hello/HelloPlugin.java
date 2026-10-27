package com.zincoid.hello;

import com.zincoid.hello.cmd.HelloCmd;
import com.zincoid.hello.tool.HelloTool;
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

    private volatile PluginContext context;

    @Override
    public String id() {
        return "hello";
    }

    @Override
    public int apiVersion() {
        return 4;
    }

    @Override
    public List<Cmd> cmds() {
        return List.of(new HelloCmd(context(), id(), version()));
    }

    @Override
    public List<Tool> tools() {
        return List.of(new HelloTool());
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
    PluginContext context() {
        PluginContext value = context;
        if (value == null) throw new IllegalStateException("Plugin not started");
        return value;
    }
}
