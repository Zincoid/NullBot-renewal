package com.zincoid.pubg;

import com.zincoid.nullbot.bot.cmd.Cmd;
import com.zincoid.nullbot.bot.plugin.Plugin;
import com.zincoid.nullbot.bot.plugin.PluginContext;
import com.zincoid.nullbot.bot.plugin.PluginResources;
import com.zincoid.pubg.cmd.PubgCmd;
import com.zincoid.pubg.service.PubgResources;

import java.nio.file.Path;
import java.util.List;

/**
 * PUBG 插件：注册 PUBG 资源地图指令，地图图片随插件 JAR 分发。
 * <p>PUBG plugin registering the PUBG resource map command; map images ship inside the plugin jar.</p>
 */
public class PubgPlugin implements Plugin {

    private volatile PubgResources resources;

    @Override
    public String id() {
        return "pubg";
    }

    @Override
    public List<Cmd> cmds() {
        return List.of(new PubgCmd(resources));
    }

    @Override
    public void start(PluginContext context) {
        Path dir = context.dataDir();
        PubgResources resources = new PubgResources(dir);
        for (String file : resources.files())
            PluginResources.extract(PubgPlugin.class, "/" + file, dir);
        this.resources = resources;
    }

    @Override
    public void stop() {
        this.resources = null;
    }
}
