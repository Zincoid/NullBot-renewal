package com.zincoid.pubg;

import com.zincoid.nullbot.bot.cmd.Cmd;
import com.zincoid.nullbot.bot.plugin.Plugin;
import com.zincoid.nullbot.bot.plugin.PluginContext;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * PUBG 插件：注册 PUBG 资源地图指令。
 * <p>PUBG plugin registering the PUBG resource map command.</p>
 */
@Slf4j
public class PubgPlugin implements Plugin {

    private volatile Path mapsDir;

    @Override
    public String id() {
        return "Pubg";
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
        Path dir = context.dataDir().resolve("maps");
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            log.warn("Cannot create PUBG maps directory: {}", dir, e);
        }
        this.mapsDir = dir;
    }

    /**
     * 返回地图图片目录。
     * <p>Returns the map image directory.</p>
     *
     * @return the map image directory
     */
    Path mapsDir() {
        Path dir = mapsDir;
        if (dir == null) throw new IllegalStateException("Plugin not started");
        return dir;
    }
}
