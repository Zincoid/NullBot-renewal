package com.zincoid.nullbot.bot.plugin;

import com.zincoid.nullbot.bot.cmd.Cmd;

import java.util.List;

/**
 * 外部插件入口：一个 JAR 提供一个实现，通过 ServiceLoader 加载。
 * <p>External plugin entry point: one implementation per JAR, loaded through ServiceLoader.</p>
 */
public interface Plugin {

    /**
     * 插件唯一标识，用于日志和插件数据目录。
     * <p>Unique plugin id, used for logs and the plugin data directory.</p>
     *
     * @return the plugin id
     */
    String id();

    /**
     * 插件版本。
     * <p>Plugin version.</p>
     *
     * @return the plugin version
     */
    String version();

    /**
     * 插件适配的宿主 API 版本。
     * <p>Host API version the plugin targets.</p>
     *
     * @return the API version
     */
    default int apiVersion() {
        return 1;
    }

    /**
     * 插件提供的指令，在 {@link #start(PluginContext)} 之后调用。
     * <p>Commands provided by the plugin, requested after {@link #start(PluginContext)}.</p>
     *
     * @return the commands, may be empty
     */
    List<Cmd> cmds();

    /**
     * 插件启动，在获取和注册指令前调用。
     * <p>Starts the plugin before its commands are requested and registered.</p>
     *
     * @param context the host services exposed to the plugin
     */
    default void start(PluginContext context) {
    }

    /**
     * 插件停止，在卸载时调用。
     * <p>Stops the plugin when it is unloaded.</p>
     */
    default void stop() {
    }
}
