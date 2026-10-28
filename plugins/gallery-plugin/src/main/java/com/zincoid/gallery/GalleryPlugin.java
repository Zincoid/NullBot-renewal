package com.zincoid.gallery;

import com.zincoid.gallery.cmd.AnimeCmd;
import com.zincoid.gallery.cmd.ZincoidCmd;
import com.zincoid.gallery.service.GalleryResources;
import com.zincoid.nullbot.bot.cmd.Cmd;
import com.zincoid.nullbot.bot.plugin.Plugin;
import com.zincoid.nullbot.bot.plugin.PluginContext;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * 图库插件：提供二次元、Zincoid 等图库指令，共用插件数据目录。
 * <p>Gallery plugin: provides anime, Zincoid, and other gallery commands over a plugin data directory.</p>
 */
@Slf4j
public class GalleryPlugin implements Plugin {

    private volatile GalleryResources images;

    @Override
    public String id() {
        return "gallery";
    }

    @Override
    public List<Cmd> cmds() {
        return List.of(new AnimeCmd(images), new ZincoidCmd(images));
    }

    @Override
    public void start(PluginContext context) {
        Path dataDir = context.dataDir();
        try {
            Files.createDirectories(dataDir);
        } catch (IOException e) {
            log.warn("Cannot create gallery resource directory: {}", dataDir, e);
        }
        this.images = new GalleryResources(dataDir);
        log.info("Gallery resources: {}", dataDir);
    }

    @Override
    public void stop() {
        this.images = null;
    }
}
