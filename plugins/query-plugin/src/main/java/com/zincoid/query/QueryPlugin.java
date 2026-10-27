package com.zincoid.query;

import com.zincoid.nullbot.bot.cmd.Cmd;
import com.zincoid.nullbot.bot.plugin.Plugin;
import com.zincoid.nullbot.bot.plugin.PluginContext;
import com.zincoid.query.cmd.AaCmd;
import com.zincoid.query.cmd.PrtsCmd;
import com.zincoid.query.cmd.TraceCmd;
import com.zincoid.query.config.QueryConfig;
import com.zincoid.query.service.AaService;
import com.zincoid.query.service.PrtsService;
import com.zincoid.query.service.TraceService;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 查询插件：注册 AA、PRTS 与图片溯源指令。
 * <p>Query plugin registering the AA, PRTS, and image trace commands.</p>
 */
@Slf4j
public class QueryPlugin implements Plugin {

    private volatile QueryConfig config;
    private volatile AaService aa;
    private volatile PrtsService prts;
    private volatile TraceService trace;

    @Override
    public String id() {
        return "query";
    }

    @Override
    public String version() {
        return "1.0";
    }

    @Override
    public List<Cmd> cmds() {
        return List.of(
                new AaCmd(aa(), config()),
                new PrtsCmd(prts()),
                new TraceCmd(trace(), config()));
    }

    @Override
    public void start(PluginContext context) {
        QueryConfig loaded = QueryConfig.load(context.dataDir());
        this.config = loaded;
        this.aa = new AaService(loaded);
        this.prts = new PrtsService(loaded);
        this.trace = new TraceService(loaded);
        log.info("Query config: aaKey={}, sauceNaoKey={}, aaTtl={}s, prtsTtl={}s",
                !loaded.aaApiKey().isBlank(), !loaded.sauceNaoApiKey().isBlank(),
                loaded.aaCacheTtl().toSeconds(), loaded.prtsCacheTtl().toSeconds());
    }

    @Override
    public void stop() {
        this.aa = null;
        this.prts = null;
        this.trace = null;
        this.config = null;
    }

    /**
     * 返回 AA 服务。
     * <p>Returns the AA service.</p>
     *
     * @return the AA service
     */
    AaService aa() {
        AaService service = aa;
        if (service == null) throw new IllegalStateException("Plugin not started");
        return service;
    }

    /**
     * 返回 PRTS 服务。
     * <p>Returns the PRTS service.</p>
     *
     * @return the PRTS service
     */
    PrtsService prts() {
        PrtsService service = prts;
        if (service == null) throw new IllegalStateException("Plugin not started");
        return service;
    }

    /**
     * 返回图片溯源服务。
     * <p>Returns the image trace service.</p>
     *
     * @return the trace service
     */
    TraceService trace() {
        TraceService service = trace;
        if (service == null) throw new IllegalStateException("Plugin not started");
        return service;
    }

    /**
     * 返回插件配置。
     * <p>Returns the plugin configuration.</p>
     *
     * @return the plugin configuration
     */
    QueryConfig config() {
        QueryConfig loaded = config;
        if (loaded == null) throw new IllegalStateException("Plugin not started");
        return loaded;
    }
}
