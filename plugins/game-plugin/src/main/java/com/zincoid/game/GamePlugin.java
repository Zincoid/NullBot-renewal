package com.zincoid.game;

import com.zincoid.nullbot.bot.cmd.Cmd;
import com.zincoid.nullbot.bot.plugin.Plugin;
import com.zincoid.nullbot.bot.plugin.PluginContext;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * 游戏插件：Wife、Guess 与 Quiz 共用一个插件数据目录。
 * <p>Game plugin: Wife, Guess, and Quiz share one plugin data directory.</p>
 */
@Slf4j
public class GamePlugin implements Plugin {

    private volatile PluginContext context;
    private volatile GameResources resources;

    @Override
    public String id() {
        return "game";
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
        return List.of(new WifeCmd(this), new GuessCmd(this), new QuizCmd(this));
    }

    @Override
    public void start(PluginContext context) {
        Path dataDir = context.dataDir();
        try {
            Files.createDirectories(dataDir);
        } catch (IOException e) {
            log.warn("Cannot create game resource directory: {}", dataDir, e);
        }
        this.resources = new GameResources(dataDir);
        this.context = context;
        log.info("Game resources: {}", dataDir);
    }

    /**
     * 返回插件上下文。
     * <p>Returns the plugin context.</p>
     *
     * @return the plugin context
     */
    PluginContext context() {
        PluginContext ctx = context;
        if (ctx == null) throw new IllegalStateException("Plugin not started");
        return ctx;
    }

    /**
     * 返回共享资源索引。
     * <p>Returns the shared resource index.</p>
     *
     * @return the resource index
     */
    GameResources resources() {
        GameResources index = resources;
        if (index == null) throw new IllegalStateException("Plugin not started");
        return index;
    }
}
