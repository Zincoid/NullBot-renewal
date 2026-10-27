package com.zincoid.gd;

import com.zincoid.gd.binding.GdBindings;
import com.zincoid.gd.cmd.GdCmd;
import com.zincoid.gd.cmd.GddlCmd;
import com.zincoid.gd.config.GdConfig;
import com.zincoid.gd.render.GdImagePresenter;
import com.zincoid.gd.render.GdPresenter;
import com.zincoid.gd.render.GdTextPresenter;
import com.zincoid.gd.service.GdLocalService;
import com.zincoid.gd.service.GdService;
import com.zincoid.gd.store.GdStore;
import com.zincoid.nullbot.bot.cmd.Cmd;
import com.zincoid.nullbot.bot.plugin.Plugin;
import com.zincoid.nullbot.bot.plugin.PluginContext;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * GD 插件：注册几何冲刺查询、本地进度与 GDDL 指令。
 * <p>GD plugin registering Geometry Dash queries, local progress, and GDDL commands.</p>
 */
@Slf4j
public class GdPlugin implements Plugin {

    private volatile PluginContext context;
    private volatile GdService service;
    private volatile GdPresenter presenter;
    private volatile GdBindings bindings;
    private volatile GdLocalService local;

    @Override
    public String id() {
        return "gd";
    }

    @Override
    public int apiVersion() {
        return 3;
    }

    @Override
    public List<Cmd> cmds() {
        return List.of(
                new GdCmd(service(), presenter(), context(), local()),
                new GddlCmd(service(), presenter(), bindings()));
    }

    @Override
    public void start(PluginContext context) {
        GdConfig loaded = GdConfig.load(context.dataDir());
        GdService service = new GdService(loaded);
        GdStore store = GdStore.open(context.dataDir());
        try {
            GdBindings bindings = GdBindings.load(context.dataDir());
            GdPresenter text = new GdTextPresenter();
            GdPresenter presenter;
            try {
                presenter = new GdImagePresenter(context.renderer(), text);
            } catch (RuntimeException e) {
                log.warn("GD image renderer unavailable; using text output", e);
                presenter = text;
            }
            GdLocalService local = new GdLocalService(store, service, loaded);

            this.context = context;
            this.service = service;
            this.bindings = bindings;
            this.local = local;
            this.presenter = presenter;
            log.info("GD config: gddl={}, thumbnail={}, cacheTtl={}s, tierCacheTtl={}s, "
                            + "unratedCacheTtl={}s, tierExponent={}, connectTimeout={}s, "
                            + "requestTimeout={}s, retries={}, proxy={}",
                    loaded.gddlBase(), loaded.thumbnailBase(),
                    loaded.cacheTtl().toSeconds(), loaded.tierCacheTtl().toSeconds(),
                    loaded.unratedCacheTtl().toSeconds(), loaded.tierExponent(),
                    loaded.connectTimeout().toSeconds(), loaded.requestTimeout().toSeconds(),
                    loaded.retries(), loaded.proxy().isBlank() ? "none" : "set");
        } catch (RuntimeException e) {
            store.close();
            throw e;
        }
    }

    @Override
    public void stop() {
        GdLocalService current = this.local;
        if (current != null) current.close();
        this.local = null;
        this.presenter = null;
        this.service = null;
        this.bindings = null;
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

    /**
     * 返回数据服务。
     * <p>Returns the data service.</p>
     *
     * @return the data service
     */
    GdService service() {
        GdService value = service;
        if (value == null) throw new IllegalStateException("Plugin not started");
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
        if (value == null) throw new IllegalStateException("Plugin not started");
        return value;
    }

    /**
     * 返回 GDDL 用户绑定存储。
     * <p>Returns the GDDL user binding store.</p>
     *
     * @return the binding store
     */
    GdBindings bindings() {
        GdBindings value = bindings;
        if (value == null) throw new IllegalStateException("Plugin not started");
        return value;
    }

    /**
     * 返回本地进度服务。
     * <p>Returns the local progress service.</p>
     *
     * @return the local progress service
     */
    GdLocalService local() {
        GdLocalService value = local;
        if (value == null) throw new IllegalStateException("Plugin not started");
        return value;
    }
}
