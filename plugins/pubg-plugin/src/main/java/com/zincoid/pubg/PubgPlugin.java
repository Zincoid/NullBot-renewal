package com.zincoid.pubg;

import com.zincoid.nullbot.bot.cmd.Cmd;
import com.zincoid.nullbot.bot.plugin.Plugin;
import com.zincoid.nullbot.bot.plugin.PluginContext;
import com.zincoid.nullbot.bot.plugin.PluginResources;
import com.zincoid.pubg.cmd.PubgCmd;

import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * PUBG 插件：注册 PUBG 资源地图指令，地图图片随插件 JAR 分发。
 * <p>PUBG plugin registering the PUBG resource map command; map images ship inside the plugin jar.</p>
 */
public class PubgPlugin implements Plugin {

    private volatile Path dataDir;

    @Override
    public String id() {
        return "pubg";
    }

    @Override
    public List<Cmd> cmds() {
        return List.of(new PubgCmd(dataDir()));
    }

    @Override
    public void start(PluginContext context) {
        Path dir = context.dataDir();
        for (String file : new LinkedHashSet<>(PubgCmd.MAPS.values()))
            PluginResources.extract(PubgPlugin.class, "/" + file, dir);
        this.dataDir = dir;
    }

    @Override
    public void stop() {
        this.dataDir = null;
    }

    /**
     * 返回插件数据目录。
     * <p>Returns the plugin data directory.</p>
     *
     * @return the plugin data directory
     */
    Path dataDir() {
        Path dir = dataDir;
        if (dir == null) throw new IllegalStateException("Plugin not started");
        return dir;
    }
}
