package com.zincoid.gd;

import com.zincoid.nullbot.bot.cmd.Cmd;
import com.zincoid.nullbot.bot.plugin.Plugin;
import com.zincoid.nullbot.bot.plugin.PluginContext;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * GD 插件：注册几何冲刺查询指令。
 * <p>GD plugin registering the Geometry Dash query command.</p>
 */
@Slf4j
public class GdPlugin implements Plugin {

    private volatile PluginContext context;
    private volatile GdService service;
    private volatile GdPresenter presenter;

    @Override
    public String id() {
        return "gd";
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
        return List.of(new GdCmd(this));
    }

    @Override
    public void start(PluginContext context) {
        GdConfig loaded = GdConfig.load(context.dataDir());
        this.context = context;
        this.service = new GdService(loaded);
        GdPresenter text = new GdTextPresenter();
        try {
            this.presenter = new GdImagePresenter(context.renderer(), text);
        } catch (RuntimeException e) {
            log.warn("GD image renderer unavailable; using text output", e);
            this.presenter = text;
        }
        log.info("GD config: gddl={}, thumbnail={}, cacheTtl={}s, tierExponent={}",
                loaded.gddlBase(), loaded.thumbnailBase(),
                loaded.cacheTtl().toSeconds(), loaded.tierExponent());
    }

    @Override
    public void stop() {
        this.presenter = null;
        this.service = null;
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
        if (value == null) throw new GdException("Plugin not started");
        return value;
    }

    /**
     * 返回数据服务。
     * <p>Returns the data service.</p>
     *
     * @return the data service
     */
    GdService service() {
        GdService value = service;
        if (value == null) throw new GdException("Plugin not started");
        return value;
    }

    /**
     * 返回输出呈现器。
     * <p>Returns the output presenter.</p>
     *
     * @return the output presenter
     */
    GdPresenter presenter() {
        GdPresenter value = presenter;
        if (value == null) throw new GdException("Plugin not started");
        return value;
    }
}
