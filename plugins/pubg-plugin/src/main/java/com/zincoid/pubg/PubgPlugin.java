package com.zincoid.pubg;

import com.zincoid.nullbot.bot.cmd.Cmd;
import com.zincoid.nullbot.bot.plugin.Plugin;
import com.zincoid.nullbot.bot.plugin.PluginContext;
import com.zincoid.nullbot.bot.plugin.PluginResources;

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
    public String version() {
        return "1.0";
    }

    @Override
    public List<Cmd> cmds() {
        return List.of(new PubgCmd(this));
    }

    @Override
    public void start(PluginContext context) {
        Path dir = context.dataDir();
        for (String file : new LinkedHashSet<>(PubgCmd.MAPS.values()))
            PluginResources.extract(PubgPlugin.class, "/" + file, dir);
        this.dataDir = dir;
    }

    /**
     * 返回已解压的地图图片路径。
     * <p>Returns the extracted map image path.</p>
     *
     * @param file the map image filename
     * @return the map image path
     */
    Path mapPath(String file) {
        return dataDir.resolve(file);
    }
}
